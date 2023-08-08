package ru.blogic.muzedodevwebutils.command;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.SSHService;
import ru.blogic.muzedodevwebutils.info.InfoService;
import ru.blogic.muzedodevwebutils.server.MuzedoServer;
import ru.blogic.muzedodevwebutils.server.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.server.MuzedoServerService;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CommandService {
    private final SSHService sshService;
    private final MuzedoServerDao muzedoServerDao;
    private final MuzedoServerService muzedoServerService;
    private final InfoService infoService;
    private final CommandDao commandDao;
    private final ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(4);

    public CommandService(
        SSHService sshService,
        MuzedoServerDao muzedoServerDao,
        MuzedoServerService muzedoServerService,
        InfoService infoService,
        CommandDao commandDao
    ) {
        this.sshService = sshService;
        this.muzedoServerDao = muzedoServerDao;
        this.muzedoServerService = muzedoServerService;
        this.infoService = infoService;
        this.commandDao = commandDao;
    }

    public String run(
        final CommandRunRequest commandRunRequest
    ) {
        final var muzedoServer = muzedoServerDao.get(commandRunRequest.serverId());
        final var command = commandDao.get(commandRunRequest.commandId());

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
                final var commandDelay = command.blocks() == Command.Block.NONE
                    ? 0
                    : Math.min(Math.max(0, commandRunRequest.delaySeconds()), 600);
                final var isBlockingCommand = command.blocks() != Command.Block.NONE;
                final var isScheduledCommand = commandDelay > 0;

                //region ПРОВЕРКА БЛОКИРОВАНИЯ ОПЕРАЦИИ
                if (isBlockingCommand) {
                    final var executingCommand = muzedoServer.getExecutingCommand();
                    if (executingCommand != null) {
                        throw new RuntimeException("В данный момент выполняется операция: " + executingCommand.name());
                    }

                    if (isScheduledCommand) {
                        final var scheduledCommand = muzedoServer.getScheduledCommand();
                        if (scheduledCommand != null) {
                            throw new RuntimeException("В данный момент запланирована операция: "
                                + scheduledCommand.command().name());
                        }
                    }
                }
                //endregion

                final Callable<String> commandCallable = () -> {
                    try {
                        if (isBlockingCommand)
                            muzedoServer.setExecutingCommand(command);
                        if (isScheduledCommand)
                            muzedoServer.setScheduledCommand(null);

                        //region ЗАПИСЬ СТАРТА ОПЕРАЦИИ
                        infoService.writeInfo(
                            muzedoServer.getId(),
                            command.announce()
                                ? MuzedoServer.LogEntry.Severity.CRIT
                                : MuzedoServer.LogEntry.Severity.INFO,
                            (isBlockingCommand ? "Запуск операции " : "")
                                + "\"" + command.name() + "\""
                                + (commandDelay == 0 && StringUtils.isNotBlank(commandRunRequest.comment())
                                ? " \"" + commandRunRequest.comment() + "\""
                                : "")
                        );
                        //endregion

                        //region ЗАПУСК ОПЕРАЦИИ
                        final String result;
                        if (command.shell().equals(Command.Shell.WSADMIN)) {
                            try {
                                result = sshService.executeCommand(
                                    muzedoServer.getWsadminShell(),
                                    command,
                                    muzedoServer.getExecutingCommandTimer());
                            } catch (Exception e) {
                                executorService.submit(() -> muzedoServerService.reconnectWsadminShell(muzedoServer));
                                throw new RuntimeException("#executeCommand ошибка выполнения WsAdmin команды: " + e.getMessage());
                            }
                        } else if (command.shell().equals(Command.Shell.SSH)) {
                            result = sshService.executeCommand(
                                muzedoServer.getSshClientSession(),
                                command,
                                muzedoServer.getExecutingCommandTimer());
                        } else {
                            result = "";
                        }
                        //endregion

                        //region ЗАПИСЬ ЗАВЕРШЕНИЯ ОПЕРАЦИИ
                        if (isBlockingCommand) {
                            infoService.writeInfo(
                                muzedoServer.getId(),
                                command.announce()
                                    ? MuzedoServer.LogEntry.Severity.CRIT
                                    : MuzedoServer.LogEntry.Severity.INFO,
                                "Завершено \"" + command.name() + "\" за " + muzedoServer.getExecutingCommandTimer().get() + " сек.");
                            muzedoServer.getExecutingCommandTimer().set(0);
                        }
                        //endregion

                        return result;
                    } catch (Exception e) {
                        infoService.writeInfo(
                            muzedoServer.getId(),
                            MuzedoServer.LogEntry.Severity.CRIT,
                            "Ошибка при выполнении операции \"" + command.name()
                                + "\": " + e.getMessage());
                        throw new RuntimeException(e);
                    } finally {
                        if (isBlockingCommand)
                            muzedoServer.setExecutingCommand(null);
                        if (command.blocks() == Command.Block.SERVER)
                            muzedoServerService.reconnectWsadminShell(muzedoServer);
                    }
                };

                if (isScheduledCommand) {
                    //region ПЛАНИРОВАНИЕ ОПЕРАЦИИ
                    infoService.writeInfo(
                        muzedoServer.getId(),
                        MuzedoServer.LogEntry.Severity.CRIT,
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
                            commandCallable,
                            commandDelay,
                            TimeUnit.SECONDS),
                        commandCallable
                    ));
                    return "Scheduled";
                    //endregion
                } else {
                    return commandCallable.call();
                }
            } finally {
                muzedoServer.getCommandSchedulingLock().unlock();
            }
        } catch (Exception e) {
            infoService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.LogEntry.Severity.CRIT,
                "Ошибка при планировании операции \"" + command.name()
                    + "\": " + e.getMessage());
            throw new RuntimeException("#run Не удалось запланировать операцию: " + e.getMessage(), e);
        }
    }

    public void delay(
        final CommandDelayRequest commandDelayRequest
    ) {
        final var muzedoServer = muzedoServerDao.get(commandDelayRequest.serverId());

        try {
            final var scheduledCommand = muzedoServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("Нет запланированной операции");
            }

            final var currentDelay = scheduledCommand.future().getDelay(TimeUnit.SECONDS);
            final var command = muzedoServer.getScheduledCommand().command();
            final var callable = muzedoServer.getScheduledCommand().callable();

            this.cancel(
                new CommandCancelRequest(
                    commandDelayRequest.serverId(),
                    commandDelayRequest.comment(),
                    true));

            final var delayPlus = Math.min(600, Math.max(0, commandDelayRequest.delaySeconds()));
            final var newDelay = Math.min(600, currentDelay + delayPlus);

            muzedoServer.setScheduledCommand(new MuzedoServer.ScheduledCommand(
                command,
                executorService.schedule(
                    callable,
                    newDelay,
                    TimeUnit.SECONDS),
                callable
            ));

            infoService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.LogEntry.Severity.CRIT,
                "Отложена операция \"" + command.name() + "\""
                    + (StringUtils.isNotBlank(commandDelayRequest.comment())
                    ? ": \"" + commandDelayRequest.comment() + "\""
                    : "")
                    + " на " + delayPlus + " сек. "
                    + "(осталось " + newDelay + " сек.)"
            );
        } catch (Exception e) {
            infoService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.LogEntry.Severity.CRIT,
                "Ошибка при откладывании операции: " + e.getMessage());
            throw new RuntimeException("#delay Не удалось отложить запланированную операцию: " + e.getMessage(), e);
        }
    }

    public void cancel(
        final CommandCancelRequest commandCancelRequest
    ) {
        final var muzedoServer = muzedoServerDao.get(commandCancelRequest.serverId());

        try {
            final var scheduledCommand = muzedoServer.getScheduledCommand();
            if (scheduledCommand == null) {
                throw new RuntimeException("В данный момент нет операции на сервере");
            }

            if (scheduledCommand.future().getDelay(TimeUnit.MILLISECONDS) <= 1000) {
                throw new RuntimeException("Отменять операцию уже поздно");
            }

            final var cancelled = scheduledCommand.future().cancel(false);
            if (!cancelled) {
                throw new RuntimeException("Не удалось отменить операцию");
            }

            final var commandName = muzedoServer.getScheduledCommand().command().name();
            muzedoServer.setScheduledCommand(null);

            if (!commandCancelRequest.silent()) {
                infoService.writeInfo(
                    muzedoServer.getId(),
                    MuzedoServer.LogEntry.Severity.CRIT,
                    "Отменена операция \"" + commandName + "\""
                        + (StringUtils.isNotBlank(commandCancelRequest.comment())
                        ? ": \"" + commandCancelRequest.comment() + "\""
                        : "")
                );
            }
        } catch (Exception e) {
            infoService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.LogEntry.Severity.CRIT,
                "Ошибка при отмене операции: " + e.getMessage());
            throw new RuntimeException("#cancel Не удалось отменить запланированную операцию:" + e.getMessage(), e);
        }
    }
}
