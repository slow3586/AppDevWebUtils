package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
            muzedoServerDao.getAll()
                .forEach(s -> {
                    log.debug(this.executeCommandShell(s,
                        "cd /root/deploy/",
                        "#"));
                    log.debug(this.executeCommandShell(s,
                        "ls",
                        "#"));
                    log.debug(this.executeCommandShell(s,
                        "./wsadmin_extra.sh",
                        ">"));
                });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String run(
        final RunCommandRequest runCommandRequest
    ) {
        final var muzedoServer = muzedoServerDao.get(runCommandRequest.serverId());
        final var command = commandDao.get(runCommandRequest.commandId());
        try {
            if (command.type().equals(Command.Type.WSADMIN)) {
                return executeCommandShell(muzedoServer, runCommandRequest.commandId(), ">");
            } else {
                infoService.writeInfo(
                    muzedoServer.id,
                    command.effect().equals(Command.Effect.WS_CRIT)
                        ? InfoEntry.Severity.CRIT
                        : InfoEntry.Severity.INFO,
                    "Запущено \""
                        + command.name()
                        + (runCommandRequest.delaySeconds() > 0
                        ? "через " + runCommandRequest.delaySeconds() + " секунд "
                        : "")
                        + (StringUtils.isNotBlank(runCommandRequest.comment())
                        ? "\": " + runCommandRequest.comment()
                        : "")
                );
                return sshService.executeCommand(muzedoServer.clientSession, runCommandRequest.commandId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommandShell(
        final MuzedoServer muzedoServer,
        final String command,
        final String readySymbol
    ) {
        try {
            final var currentRef = muzedoServer.getChannelShellCurrentCommand();

            if (!currentRef.compareAndSet("", currentRef.get())) {
                throw new RuntimeException("В даннный момент выполняется операция: " + currentRef.get());
            }

            infoService.writeInfo(
                muzedoServer.id,
                InfoEntry.Severity.CRIT,
                "Запущен " + command + " на " + muzedoServer.host);
            currentRef.set(command);
            final var result = sshService.executeCommand(
                muzedoServer.channelShell,
                command,
                readySymbol);

            currentRef.set("");
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
