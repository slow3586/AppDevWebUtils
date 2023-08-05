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
        Executors.newSingleThreadScheduledExecutor();

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

    @PostConstruct
    public void postConstruct() {
        try {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String run(
        final CommandRunRequest commandRunRequest
    ) {
        try {
            final var muzedoServer = muzedoServerDao.get(commandRunRequest.serverId());

            if(muzedoServer.getWsadminShell() == null || muzedoServer.getWsadminShell().isClosing()) {
                throw new RuntimeException("Wsadmin закрыт!");
            }

            final var command = commandDao.get(commandRunRequest.commandId());

            final var currentRef = muzedoServer.getWsadminShellCommand();
            if (command.effect() != Command.Effect.NONE) {
                if (!currentRef.compareAndSet(null, currentRef.get())) {
                    throw new RuntimeException("В даннный момент выполняется операция: " + currentRef.get().name());
                }
                currentRef.set(command);
            }

            final var delay = Math.min(Math.max(0, commandRunRequest.delaySeconds()), 600);

            final Callable<String> callable = () -> {
                muzedoServer.setScheduledCommandFuture(null);
                muzedoServer.setScheduledCommandActive(true);

                infoService.writeInfo(
                    muzedoServer.getId(),
                    (command.effect() == Command.Effect.WS_BLOCK
                        || command.effect() == Command.Effect.SERVER_BLOCK)
                        ? MuzedoServer.LogEntry.Severity.CRIT
                        : MuzedoServer.LogEntry.Severity.INFO,
                    (!command.shell().equals(Command.Shell.NONE) ? "Запуск операции " : "")
                        + "\"" + command.name() + "\""
                        + (delay == 0 && StringUtils.isNotBlank(commandRunRequest.comment())
                        ? " \"" + commandRunRequest.comment() + "\""
                        : "")
                );

                final String result;
                if (command.shell().equals(Command.Shell.WSADMIN)) {
                    result = sshService.executeCommand(
                        muzedoServer.getWsadminShell(),
                        command.command(),
                        ">");
                } else if (command.shell().equals(Command.Shell.SSH)) {
                    result = sshService.executeCommand(
                        muzedoServer.getClientSession(),
                        command.command());
                } else {
                    result = "";
                }

                if (!command.shell().equals(Command.Shell.NONE)) {
                    currentRef.set(null);
                    infoService.writeInfo(
                        muzedoServer.getId(),
                        command.effect() == Command.Effect.WS_BLOCK
                            || command.effect() == Command.Effect.SERVER_BLOCK
                            ? MuzedoServer.LogEntry.Severity.CRIT
                            : MuzedoServer.LogEntry.Severity.INFO,
                        "Завершено \"" + command.name() + "\"");
                }

                if (command.effect() == Command.Effect.SERVER_BLOCK) {
                    muzedoServerService.reconnectWsadminShell(muzedoServer);
                }

                muzedoServer.setScheduledCommandActive(false);
                return result;
            };

            if (delay > 0) {
                infoService.writeInfo(
                    muzedoServer.getId(),
                    command.effect().equals(Command.Effect.WS_BLOCK)
                        ? MuzedoServer.LogEntry.Severity.CRIT
                        : MuzedoServer.LogEntry.Severity.INFO,
                    "Запланирована операция "
                        + "\"" + command.name() + "\""
                        + (StringUtils.isNotBlank(commandRunRequest.comment())
                        ? ": \"" + commandRunRequest.comment() + "\""
                        : "")
                        + " через " + commandRunRequest.delaySeconds() + " сек."
                );
                final var schedule = executorService.schedule(
                    callable,
                    delay,
                    TimeUnit.SECONDS);
                muzedoServer.setScheduledCommandFuture(schedule);
                muzedoServer.setScheduledCallable(callable);
                muzedoServer.setScheduledCommand(command);
                return "Scheduled";
            } else {
                return callable.call();
            }
        } catch (Exception e) {
            throw new RuntimeException("#run Не удалось запланировать операцию: " + e.getMessage(), e);
        }
    }

    public void delay(
        final CommandDelayRequest commandDelayRequest
    ) {
        try {
            final var muzedoServer = muzedoServerDao.get(commandDelayRequest.serverId());

            final var scheduledCommand = muzedoServer.getScheduledCommandFuture();
            if (scheduledCommand == null) {
                throw new RuntimeException("Нет запланированной операции");
            }

            final var currentDelay = scheduledCommand.getDelay(TimeUnit.SECONDS);
            final var command = muzedoServer.getScheduledCommand();
            final var callable = muzedoServer.getScheduledCallable();
            final var shellCommand = muzedoServer.getWsadminShellCommand().get();

            this.cancel(
                new CommandCancelRequest(
                    commandDelayRequest.serverId(),
                    commandDelayRequest.comment(),
                    true));

            final var newDelay = currentDelay + commandDelayRequest.delaySeconds();
            final var newSchedule = executorService.schedule(
                callable,
                newDelay,
                TimeUnit.SECONDS);

            muzedoServer.setScheduledCommand(command);
            muzedoServer.setScheduledCommandFuture(newSchedule);
            muzedoServer.setScheduledCallable(callable);
            muzedoServer.getWsadminShellCommand().set(shellCommand);

            infoService.writeInfo(
                muzedoServer.getId(),
                MuzedoServer.LogEntry.Severity.CRIT,
                "Отложена операция \"" + command.name() + "\""
                    + (StringUtils.isNotBlank(commandDelayRequest.comment())
                    ? ": \"" + commandDelayRequest.comment() + "\""
                    : "")
                    + "(осталось " + newDelay + " сек)"
            );
        } catch (Exception e) {
            throw new RuntimeException("#delay Не удалось отложить запланированную операцию: " + e.getMessage(), e);
        }
    }

    public void cancel(
        final CommandCancelRequest commandCancelRequest
    ) {
        try {
            final var muzedoServer = muzedoServerDao.get(commandCancelRequest.serverId());

            final var scheduledCommand = muzedoServer.getScheduledCommandFuture();
            if (scheduledCommand == null) {
                throw new RuntimeException("В данный момент нет операции на сервере");
            }

            if (scheduledCommand.getDelay(TimeUnit.SECONDS) <= 1) {
                throw new RuntimeException("Отменять операцию уже поздно.");
            }

            final var cancelled = scheduledCommand.cancel(false);
            if (!cancelled) {
                throw new RuntimeException("Не удалось отменить операцию.");
            }

            final var commandName = muzedoServer.getScheduledCommand().name();
            muzedoServer.setScheduledCommand(null);
            muzedoServer.setScheduledCommandFuture(null);
            muzedoServer.getWsadminShellCommand().set(null);
            muzedoServer.setScheduledCallable(null);

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
            throw new RuntimeException("#cancel Не удалось отменить запланированную операцию:" + e.getMessage(), e);
        }
    }
}
