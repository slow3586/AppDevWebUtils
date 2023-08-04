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

@Service
@Slf4j
public class CommandService {
    private final SSHService sshService;
    private final MuzedoServerDao muzedoServerDao;
    private final MuzedoServerService muzedoServerService;
    private final InfoService infoService;
    private final CommandDao commandDao;

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
            final var command = commandDao.get(commandRunRequest.commandId());
            infoService.writeInfo(
                muzedoServer.getId(),
                command.effect().equals(Command.Effect.WS_CRIT)
                    ? MuzedoServer.LogEntry.Severity.CRIT
                    : MuzedoServer.LogEntry.Severity.INFO,
                (!command.type().equals(Command.Type.NONE) ? "Запуск " : "")
                    + "\"" + command.name() + "\""
                    + (StringUtils.isNotBlank(commandRunRequest.comment())
                    ? " \"" + commandRunRequest.comment() + "\""
                    : "")
                    + (commandRunRequest.delaySeconds() > 0
                    ? " через " + commandRunRequest.delaySeconds() + " сек"
                    : "")
            );

            String result = "";
            if (command.type().equals(Command.Type.WSADMIN)) {
                result = executeCommandShell(muzedoServer, commandRunRequest.commandId(), ">");
            } else if (command.type().equals(Command.Type.SSH)) {
                result = sshService.executeCommand(muzedoServer.getClientSession(), commandRunRequest.commandId());
            }

            if (!command.type().equals(Command.Type.NONE)) {
                infoService.writeInfo(
                    muzedoServer.getId(),
                    command.effect().equals(Command.Effect.WS_CRIT)
                        ? MuzedoServer.LogEntry.Severity.CRIT
                        : MuzedoServer.LogEntry.Severity.INFO,
                    "Завершено \"" + command.name() + "\"");
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommandShell(
        final MuzedoServer muzedoServer,
        final String commandId,
        final String readySymbol
    ) {
        try {
            final var currentRef = muzedoServer.getChannelShellCurrentCommand();

            if (!currentRef.compareAndSet(null, currentRef.get())) {
                throw new RuntimeException("В даннный момент выполняется операция: " + currentRef.get().name());
            }

            final var command = commandDao.get(commandId);

            currentRef.set(command);
            final var result = sshService.executeCommand(
                muzedoServer.getChannelShell(),
                command.command(),
                readySymbol);

            currentRef.set(null);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
