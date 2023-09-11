package ru.blogic.muzedodevwebutils.api.command;

import io.vavr.CheckedFunction0;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.command.dao.CommandDao;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandCancelRequest;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandDelayRequest;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandRunRequest;
import ru.blogic.muzedodevwebutils.api.history.HistoryService;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerService;
import ru.blogic.muzedodevwebutils.api.muzedo.ssh.SSHService;
import ru.blogic.muzedodevwebutils.utils.TimerScheduler;
import ru.blogic.muzedodevwebutils.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommandService {
    SSHService sshService;
    MuzedoServerDao muzedoServerDao;
    MuzedoServerService muzedoServerService;
    HistoryService historyService;
    CommandDao commandDao;
    TimerScheduler timerScheduler;
    ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(4);

    public void run(
        final CommandRunRequest commandRunRequest
    ) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(commandRunRequest.serverId());
        final Command command = commandDao.get(commandRunRequest.commandId());

        try {
            if (muzedoServer.getWsadminShell() == null
                || muzedoServer.getWsadminShell().isClosing()
                || !muzedoServer.getWsadminShell().isOpen()) {
                throw new RuntimeException("Wsadmin не запущен! Необходимо дождаться его запуска (~1 мин)");
            }

            if (!muzedoServer.getCommandSchedulingLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("На сервере планируется другая операция");
            }

            try {
                final int commandDelay = !command.blocksWsadmin()
                    ? 0
                    : Utils.clamp(commandRunRequest.delaySeconds(), 0, 600);
                final boolean isBlockingCommand = command.blocksWsadmin();
                final boolean isScheduledCommand = commandDelay > 0;

                //region ПРОВЕРКА БЛОКИРОВАНИЯ ОПЕРАЦИИ
                if (isBlockingCommand) {
                    final Command executingCommand = muzedoServer.getExecutingCommand();
                    if (executingCommand != null) {
                        throw new RuntimeException("В данный момент выполняется операция: " + executingCommand.name());
                    }

                    if (isScheduledCommand) {
                        final MuzedoServer.ScheduledCommand scheduledCommand = muzedoServer.getScheduledCommand();
                        if (scheduledCommand != null) {
                            throw new RuntimeException("В данный момент запланирована операция: "
                                + scheduledCommand.command().name());
                        }
                    }
                }
                //endregion

                final Runnable commandRunnable = () -> {
                    try {
                        if (isBlockingCommand) {
                            muzedoServer.setExecutingCommand(command);
                            muzedoServer.setExecutingCommandTimer(timerScheduler.start());
                        }
                        if (isScheduledCommand) {
                            muzedoServer.setScheduledCommand(null);
                        }

                        //region ЗАПИСЬ СТАРТА ОПЕРАЦИИ
                        historyService.addHistoryEntry(
                            muzedoServer.getId(),
                            command.announce()
                                ? MuzedoServer.HistoryEntry.Severity.CRIT
                                : MuzedoServer.HistoryEntry.Severity.INFO,
                            (isBlockingCommand ? "Запуск операции " : "")
                                + "\"" + command.name() + "\""
                                + (commandDelay == 0 && StringUtils.isNotBlank(commandRunRequest.comment())
                                ? " \"" + commandRunRequest.comment() + "\""
                                : "")
                        );
                        //endregion

                        //region ЗАПУСК ОПЕРАЦИИ
                        if (command.shell().equals(Command.Shell.WSADMIN)) {
                            CheckedFunction0<SSHService.ExecuteCommandResult> execute = () ->
                                sshService.executeCommand(
                                    muzedoServer.getWsadminShell(),
                                    command,
                                    List.empty()).block();
                            Try.of(execute)
                                .onFailure(e -> {
                                    log.error("#run ошибка при первом запуске, запускаю вторую попытку...", e);
                                    historyService.addHistoryEntry(
                                        muzedoServer.getId(),
                                        MuzedoServer.HistoryEntry.Severity.CRIT,
                                        "Ошибка при выполнении операции \"" + command.name()
                                            + "\": " + e.getMessage()
                                            + ", запускаю вторую попытку...");
                                }).getOrElse(() -> {
                                    muzedoServerService.reconnectWsadminShell(muzedoServer);
                                    return Try.of(execute)
                                        .getOrElseThrow((e1) -> new RuntimeException(
                                            "#executeCommand ошибка выполнения WsAdmin команды: "
                                                + e1.getMessage(), e1));
                                });
                        } else if (command.shell().equals(Command.Shell.SSH)) {
                            sshService.executeCommand(
                                muzedoServer.getSshClientSession(),
                                command,
                                List.empty());
                        } else {
                            return;
                        }
                        //endregion

                        //region ЗАПИСЬ ЗАВЕРШЕНИЯ ОПЕРАЦИИ
                        if (isBlockingCommand) {
                            historyService.addHistoryEntry(
                                muzedoServer.getId(),
                                command.announce()
                                    ? MuzedoServer.HistoryEntry.Severity.CRIT
                                    : MuzedoServer.HistoryEntry.Severity.INFO,
                                "Завершено \""
                                    + command.name()
                                    + "\" за "
                                    + muzedoServer.getExecutingCommandTimer().getTime()
                                    + " сек.");
                        }
                        //endregion
                    } catch (Exception e) {
                        historyService.addHistoryEntry(
                            muzedoServer.getId(),
                            MuzedoServer.HistoryEntry.Severity.CRIT,
                            "Ошибка при выполнении операции \"" + command.name()
                                + "\": " + e.getMessage());
                        throw new RuntimeException(e);
                    } finally {
                        if (isBlockingCommand) {
                            muzedoServer.setExecutingCommand(null);
                            timerScheduler.stop(muzedoServer.getExecutingCommandTimer());
                        }
                        if (command.blocksWsadmin())
                            muzedoServerService.reconnectWsadminShell(muzedoServer);
                    }
                };

                if (isScheduledCommand) {
                    //region ПЛАНИРОВАНИЕ ОПЕРАЦИИ
                    historyService.addHistoryEntry(
                        muzedoServer.getId(),
                        MuzedoServer.HistoryEntry.Severity.CRIT,
                        "Запланирована операция "
                            + "\"" + command.name() + "\""
                            + (StringUtils.isNotBlank(commandRunRequest.comment())
                            ? ": \"" + commandRunRequest.comment() + "\""
                            : "")
                            + " через " + commandRunRequest.delaySeconds() + " сек."
                    );
                    muzedoServer.setScheduledCommand(new MuzedoServer.ScheduledCommand(
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
                muzedoServer.getCommandSchedulingLock().unlock();
            }
        } catch (Exception e) {
            historyService.addHistoryEntry(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                "Ошибка при планировании операции \"" + command.name()
                    + "\": " + e.getMessage());
            throw new RuntimeException("#run Не удалось запланировать операцию: " + e.getMessage(), e);
        }
    }

    public void delay(
        final CommandDelayRequest commandDelayRequest
    ) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(commandDelayRequest.serverId());

        try {
            final MuzedoServer.ScheduledCommand scheduledCommand = muzedoServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("Нет запланированной операции");
            }

            final long currentDelay = scheduledCommand.future().getDelay(TimeUnit.SECONDS);
            final Command command = muzedoServer.getScheduledCommand().command();
            final Runnable callable = muzedoServer.getScheduledCommand().callable();

            this.cancel(
                new CommandCancelRequest(
                    commandDelayRequest.serverId(),
                    commandDelayRequest.comment(),
                    true));

            final int delayPlus = Utils.clamp(commandDelayRequest.delaySeconds(), 0, 600);
            final long newDelay = Utils.clamp((int) (currentDelay + delayPlus), 0, 600);

            muzedoServer.setScheduledCommand(new MuzedoServer.ScheduledCommand(
                command,
                executorService.schedule(
                    callable,
                    newDelay,
                    TimeUnit.SECONDS),
                callable
            ));

            historyService.addHistoryEntry(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                "Отложена операция \"" + command.name() + "\""
                    + (StringUtils.isNotBlank(commandDelayRequest.comment())
                    ? ": \"" + commandDelayRequest.comment() + "\""
                    : "")
                    + " на " + delayPlus + " сек. "
                    + "(осталось " + newDelay + " сек.)"
            );

        } catch (Exception e) {
            historyService.addHistoryEntry(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                "Ошибка при откладывании операции: " + e.getMessage());
            throw new RuntimeException("#delay Не удалось отложить запланированную операцию: " + e.getMessage(), e);
        }
    }

    public void cancel(
        final CommandCancelRequest commandCancelRequest
    ) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(commandCancelRequest.serverId());

        try {
            final MuzedoServer.ScheduledCommand scheduledCommand = muzedoServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("В данный момент нет операции на сервере");
            }

            if (scheduledCommand.future().getDelay(TimeUnit.MILLISECONDS) <= 1000) {
                throw new RuntimeException("Отменять операцию уже поздно");
            }

            final boolean cancelled = scheduledCommand.future().cancel(false);
            if (!cancelled) {
                throw new RuntimeException("Не удалось отменить операцию");
            }

            final String commandName = muzedoServer.getScheduledCommand().command().name();
            muzedoServer.setScheduledCommand(null);

            if (!commandCancelRequest.silent()) {
                historyService.addHistoryEntry(
                    muzedoServer.getId(),
                    MuzedoServer.HistoryEntry.Severity.CRIT,
                    "Отменена операция \"" + commandName + "\""
                        + (StringUtils.isNotBlank(commandCancelRequest.comment())
                        ? ": \"" + commandCancelRequest.comment() + "\""
                        : "")
                );
            }
        } catch (Exception e) {
            historyService.addHistoryEntry(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                "Ошибка при отмене операции: " + e.getMessage());
            throw new RuntimeException("#cancel Не удалось отменить запланированную операцию:" + e.getMessage(), e);
        }
    }
}
