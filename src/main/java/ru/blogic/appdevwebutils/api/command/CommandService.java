package ru.blogic.appdevwebutils.api.command;

import io.vavr.CheckedFunction0;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.AppServerService;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.api.app.ssh.SshService;
import ru.blogic.appdevwebutils.api.command.config.CommandConfig;
import ru.blogic.appdevwebutils.api.command.dto.CommandCancelRequest;
import ru.blogic.appdevwebutils.api.command.dto.CommandDelayRequest;
import ru.blogic.appdevwebutils.api.command.dto.CommandRunRequest;
import ru.blogic.appdevwebutils.api.history.HistoryService;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntry;
import ru.blogic.appdevwebutils.utils.TimerScheduler;
import ru.blogic.appdevwebutils.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис, отвечающий за выполнение и планирование операций пользователями на серверах приложений.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommandService {
    SshService sshService;
    AppServerConfig appServerConfig;
    AppServerService appServerService;
    HistoryService historyService;
    CommandConfig commandConfig;
    TimerScheduler timerScheduler;
    ScheduledExecutorService executorService =
        new DelegatingSecurityContextScheduledExecutorService(
            Executors.newScheduledThreadPool(4));

    /**
     * Запускает/планирует указанную операцию на указанном сервере приложения.
     */
    public void run(
        final CommandRunRequest commandRunRequest
    ) {
        final AppServer appServer = appServerConfig.get(commandRunRequest.serverId());
        final Command command = commandConfig.get(commandRunRequest.commandId());

        try {
            final boolean isBlockingCommand = command.blocksWsadmin();
            if (isBlockingCommand) {
                if (appServer.getWsadminShell() == null
                    || appServer.getWsadminShell().isClosing()
                    || !appServer.getWsadminShell().isOpen()) {
                    throw new RuntimeException("Wsadmin не запущен! Необходимо дождаться его запуска (~1 мин)");
                }

                if (!appServer.getCommandSchedulingLock().tryLock(5, TimeUnit.SECONDS)) {
                    throw new RuntimeException("На сервере планируется другая операция");
                }
            }

            try {
                final int commandDelay = !command.blocksWsadmin()
                    ? 0
                    : Utils.clamp(commandRunRequest.delaySeconds(), 0, 600);
                final boolean isScheduledCommand = commandDelay > 0;

                //region ПРОВЕРКА БЛОКИРОВАНИЯ ОПЕРАЦИИ
                if (isBlockingCommand) {
                    final Command executingCommand = appServer.getExecutingCommand();
                    if (executingCommand != null) {
                        throw new RuntimeException("В данный момент выполняется операция: " + executingCommand.name());
                    }

                    if (isScheduledCommand) {
                        final AppServer.ScheduledCommand scheduledCommand = appServer.getScheduledCommand();
                        if (scheduledCommand != null) {
                            throw new RuntimeException("В данный момент запланирована операция: "
                                + scheduledCommand.command().name());
                        }
                    }
                }
                //endregion

                final Runnable commandRunnable = new DelegatingSecurityContextRunnable(() -> {
                    try {
                        if (isBlockingCommand) {
                            appServer.setExecutingCommand(command);
                            appServer.setExecutingCommandTimer(timerScheduler.start());
                        }
                        if (isScheduledCommand) {
                            appServer.setScheduledCommand(null);
                        }

                        //region ЗАПИСЬ СТАРТА ОПЕРАЦИИ
                        historyService.addHistoryEntry(
                            appServer.getId(),
                            command.announce()
                                ? HistoryEntry.Severity.CRIT
                                : HistoryEntry.Severity.INFO,
                            (isBlockingCommand ? "Запуск операции " : "")
                                + "\"" + command.name() + "\""
                                + (commandDelay == 0 && StringUtils.isNotBlank(commandRunRequest.comment())
                                ? ": \"" + commandRunRequest.comment() + "\""
                                : "")
                        );
                        //endregion

                        //region ЗАПУСК ОПЕРАЦИИ
                        if (command.shell().equals(Command.Shell.WSADMIN)) {
                            CheckedFunction0<String> execute = () ->
                                sshService.executeCommand(
                                    appServer.getWsadminShell(),
                                    command,
                                    List.empty());
                            Try.of(execute)
                                .onFailure(e -> {
                                    log.error("#run ошибка при первом запуске, запускаю вторую попытку...", e);
                                    historyService.addHistoryEntry(
                                        appServer.getId(),
                                        HistoryEntry.Severity.CRIT,
                                        "Ошибка при выполнении операции \"" + command.name()
                                            + "\": " + e.getMessage()
                                            + ", запускаю вторую попытку...");
                                }).getOrElse(() -> {
                                    appServerService.reconnectWsadminShell(appServer);
                                    return Try.of(execute)
                                        .getOrElseThrow((e1) -> new RuntimeException(
                                            "#executeCommand ошибка выполнения WsAdmin команды: "
                                                + e1.getMessage(), e1));
                                });
                        } else if (command.shell().equals(Command.Shell.SSH)) {
                            sshService.executeCommand(
                                appServer,
                                command,
                                List.empty());
                        } else {
                            return;
                        }
                        //endregion

                        //region ЗАПИСЬ ЗАВЕРШЕНИЯ ОПЕРАЦИИ
                        if (isBlockingCommand) {
                            historyService.addHistoryEntry(
                                appServer.getId(),
                                command.announce()
                                    ? HistoryEntry.Severity.CRIT
                                    : HistoryEntry.Severity.INFO,
                                "Завершено \""
                                    + command.name()
                                    + "\" за "
                                    + appServer.getExecutingCommandTimer().getTime()
                                    + " сек.");
                        }
                        //endregion
                    } catch (Exception e) {
                        historyService.addHistoryEntry(
                            appServer.getId(),
                            HistoryEntry.Severity.CRIT,
                            "Ошибка при выполнении операции \"" + command.name()
                                + "\": " + e.getMessage());
                        throw new RuntimeException(e);
                    } finally {
                        if (isBlockingCommand) {
                            appServer.setExecutingCommand(null);
                            timerScheduler.stop(appServer.getExecutingCommandTimer());
                        }
                        if (command.blocksWsadmin())
                            appServerService.reconnectWsadminShell(appServer);
                    }
                }, SecurityContextHolder.getContext());

                if (isScheduledCommand) {
                    //region ПЛАНИРОВАНИЕ ОПЕРАЦИИ
                    historyService.addHistoryEntry(
                        appServer.getId(),
                        HistoryEntry.Severity.CRIT,
                        "Запланирована операция "
                            + "\"" + command.name() + "\""
                            + (StringUtils.isNotBlank(commandRunRequest.comment())
                            ? ": \"" + commandRunRequest.comment() + "\""
                            : "")
                            + " через " + commandRunRequest.delaySeconds() + " сек."
                    );
                    appServer.setScheduledCommand(new AppServer.ScheduledCommand(
                        command,
                        executorService.schedule(
                            commandRunnable,
                            commandDelay,
                            TimeUnit.SECONDS),
                        commandRunnable
                    ));
                    //endregion
                } else {
                    commandRunnable.run();
                }
            } finally {
                if (isBlockingCommand) {
                    appServer.getCommandSchedulingLock().unlock();
                }
            }
        } catch (Exception e) {
            historyService.addHistoryEntry(
                appServer.getId(),
                HistoryEntry.Severity.CRIT,
                "Ошибка при планировании операции \"" + command.name()
                    + "\": " + e.getMessage());
            throw new RuntimeException("#run Не удалось запланировать операцию: " + e.getMessage(), e);
        }
    }

    /**
     * Откладывает текущую запланированную операцию на указанное время.
     */
    public void delay(
        final CommandDelayRequest commandDelayRequest
    ) {
        final AppServer appServer = appServerConfig.get(commandDelayRequest.serverId());

        try {
            final AppServer.ScheduledCommand scheduledCommand = appServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("Нет запланированной операции");
            }

            final long currentDelay = scheduledCommand.scheduledFuture().getDelay(TimeUnit.SECONDS);
            final Command command = appServer.getScheduledCommand().command();
            final Runnable runnable = appServer.getScheduledCommand().runnable();

            this.cancel(
                new CommandCancelRequest(
                    commandDelayRequest.serverId(),
                    commandDelayRequest.comment(),
                    true));

            final int delayPlus = Utils.clamp(commandDelayRequest.delaySeconds(), 0, 600);
            final long newDelay = Utils.clamp((int) (currentDelay + delayPlus), 0, 600);

            appServer.setScheduledCommand(new AppServer.ScheduledCommand(
                command,
                executorService.schedule(
                    runnable,
                    newDelay,
                    TimeUnit.SECONDS),
                runnable
            ));

            historyService.addHistoryEntry(
                appServer.getId(),
                HistoryEntry.Severity.CRIT,
                "Отложена операция \"" + command.name() + "\""
                    + (StringUtils.isNotBlank(commandDelayRequest.comment())
                    ? ": \"" + commandDelayRequest.comment() + "\""
                    : "")
                    + " на " + delayPlus + " сек. "
                    + "(осталось " + newDelay + " сек.)"
            );

        } catch (Exception e) {
            historyService.addHistoryEntry(
                appServer.getId(),
                HistoryEntry.Severity.CRIT,
                "Ошибка при откладывании операции: " + e.getMessage());
            throw new RuntimeException("#delay Не удалось отложить запланированную операцию: " + e.getMessage(), e);
        }
    }

    /**
     * Отменяет текущую запланированную операцию на указанном сервере приложения.
     */
    public void cancel(
        final CommandCancelRequest commandCancelRequest
    ) {
        final AppServer appServer = appServerConfig.get(commandCancelRequest.serverId());

        try {
            final AppServer.ScheduledCommand scheduledCommand = appServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("В данный момент нет операции на сервере");
            }

            if (scheduledCommand.scheduledFuture().getDelay(TimeUnit.MILLISECONDS) <= 1000) {
                throw new RuntimeException("Отменять операцию уже поздно");
            }

            final boolean cancelled = scheduledCommand.scheduledFuture().cancel(false);
            if (!cancelled) {
                throw new RuntimeException("Не удалось отменить операцию");
            }

            final String commandName = appServer.getScheduledCommand().command().name();
            appServer.setScheduledCommand(null);

            if (!commandCancelRequest.silent()) {
                historyService.addHistoryEntry(
                    appServer.getId(),
                    HistoryEntry.Severity.CRIT,
                    "Отменена операция \"" + commandName + "\""
                        + (StringUtils.isNotBlank(commandCancelRequest.comment())
                        ? ": \"" + commandCancelRequest.comment() + "\""
                        : "")
                );
            }
        } catch (Exception e) {
            historyService.addHistoryEntry(
                appServer.getId(),
                HistoryEntry.Severity.CRIT,
                "Ошибка при отмене операции: " + e.getMessage());
            throw new RuntimeException("#cancel Не удалось отменить запланированную операцию:" + e.getMessage(), e);
        }
    }
}
