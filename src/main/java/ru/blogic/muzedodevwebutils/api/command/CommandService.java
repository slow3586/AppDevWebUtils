package ru.blogic.muzedodevwebutils.api.command;

import io.vavr.CheckedFunction0;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.command.dao.CommandDao;
import ru.blogic.muzedodevwebutils.api.history.HistoryService;
import ru.blogic.muzedodevwebutils.api.muzedo.SSHService;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandCancelRequest;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandDelayRequest;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandRunRequest;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerService;
import ru.blogic.muzedodevwebutils.utils.Timer;

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
    ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(4);

    public void run(
        final CommandRunRequest commandRunRequest
    ) {
        val muzedoServer = muzedoServerDao.get(commandRunRequest.serverId());
        val command = commandDao.get(commandRunRequest.commandId());

        try (final Timer runTimer = muzedoServer.getExecutingCommandTimer().start()) {
            if (muzedoServer.getWsadminShell() == null
                || muzedoServer.getWsadminShell().isClosing()
                || !muzedoServer.getWsadminShell().isOpen()) {
                throw new RuntimeException("Wsadmin не запущен! Необходимо дождаться его запуска (~1 мин)");
            }

            if (!muzedoServer.getCommandSchedulingLock().tryLock(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("На сервере планируется другая операция");
            }

            try {
                val commandDelay = !command.blocksWsadmin()
                    ? 0
                    : Math.min(Math.max(0, commandRunRequest.delaySeconds()), 600);
                val isBlockingCommand = command.blocksWsadmin();
                val isScheduledCommand = commandDelay > 0;

                //region ПРОВЕРКА БЛОКИРОВАНИЯ ОПЕРАЦИИ
                if (isBlockingCommand) {
                    val executingCommand = muzedoServer.getExecutingCommand();
                    if (executingCommand != null) {
                        throw new RuntimeException("В данный момент выполняется операция: " + executingCommand.name());
                    }

                    if (isScheduledCommand) {
                        val scheduledCommand = muzedoServer.getScheduledCommand();
                        if (scheduledCommand != null) {
                            throw new RuntimeException("В данный момент запланирована операция: "
                                + scheduledCommand.command().name());
                        }
                    }
                }
                //endregion

                final Runnable commandRunnable = () -> {
                    try {
                        if (isBlockingCommand)
                            muzedoServer.setExecutingCommand(command);
                        if (isScheduledCommand)
                            muzedoServer.setScheduledCommand(null);

                        //region ЗАПИСЬ СТАРТА ОПЕРАЦИИ
                        historyService.writeInfo(
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
                                .onFailure(e -> log.error("#run ошибка при первом запуске, запускаю вторую попытку...", e))
                                .getOrElse(() -> {
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
                            historyService.writeInfo(
                                muzedoServer.getId(),
                                command.announce()
                                    ? MuzedoServer.HistoryEntry.Severity.CRIT
                                    : MuzedoServer.HistoryEntry.Severity.INFO,
                                "Завершено \"" + command.name() + "\" за " + muzedoServer.getExecutingCommandTimer().getTime() + " сек.");
                        }
                        //endregion
                    } catch (Exception e) {
                        historyService.writeInfo(
                            muzedoServer.getId(),
                            MuzedoServer.HistoryEntry.Severity.CRIT,
                            "Ошибка при выполнении операции \"" + command.name()
                                + "\": " + e.getMessage());
                        throw new RuntimeException(e);
                    } finally {
                        if (isBlockingCommand)
                            muzedoServer.setExecutingCommand(null);
                        if (command.blocksWsadmin())
                            muzedoServerService.reconnectWsadminShell(muzedoServer);
                    }
                };

                if (isScheduledCommand) {
                    //region ПЛАНИРОВАНИЕ ОПЕРАЦИИ
                    historyService.writeInfo(
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
            historyService.writeInfo(
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
        val muzedoServer = muzedoServerDao.get(commandDelayRequest.serverId());

        try {
            val scheduledCommand = muzedoServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("Нет запланированной операции");
            }

            val currentDelay = scheduledCommand.future().getDelay(TimeUnit.SECONDS);
            val command = muzedoServer.getScheduledCommand().command();
            val callable = muzedoServer.getScheduledCommand().callable();

            this.cancel(
                new CommandCancelRequest(
                    commandDelayRequest.serverId(),
                    commandDelayRequest.comment(),
                    true));

            val delayPlus = Math.min(600, Math.max(0, commandDelayRequest.delaySeconds()));
            val newDelay = Math.min(600, currentDelay + delayPlus);

            muzedoServer.setScheduledCommand(new MuzedoServer.ScheduledCommand(
                command,
                executorService.schedule(
                    callable,
                    newDelay,
                    TimeUnit.SECONDS),
                callable
            ));

            historyService.writeInfo(
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
            historyService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                "Ошибка при откладывании операции: " + e.getMessage());
            throw new RuntimeException("#delay Не удалось отложить запланированную операцию: " + e.getMessage(), e);
        }
    }

    public void cancel(
        final CommandCancelRequest commandCancelRequest
    ) {
        val muzedoServer = muzedoServerDao.get(commandCancelRequest.serverId());

        try {
            val scheduledCommand = muzedoServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("В данный момент нет операции на сервере");
            }

            if (scheduledCommand.future().getDelay(TimeUnit.MILLISECONDS) <= 1000) {
                throw new RuntimeException("Отменять операцию уже поздно");
            }

            val cancelled = scheduledCommand.future().cancel(false);
            if (!cancelled) {
                throw new RuntimeException("Не удалось отменить операцию");
            }

            val commandName = muzedoServer.getScheduledCommand().command().name();
            muzedoServer.setScheduledCommand(null);

            if (!commandCancelRequest.silent()) {
                historyService.writeInfo(
                    muzedoServer.getId(),
                    MuzedoServer.HistoryEntry.Severity.CRIT,
                    "Отменена операция \"" + commandName + "\""
                        + (StringUtils.isNotBlank(commandCancelRequest.comment())
                        ? ": \"" + commandCancelRequest.comment() + "\""
                        : "")
                );
            }
        } catch (Exception e) {
            historyService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                "Ошибка при отмене операции: " + e.getMessage());
            throw new RuntimeException("#cancel Не удалось отменить запланированную операцию:" + e.getMessage(), e);
        }
    }
}
