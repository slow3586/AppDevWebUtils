package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class CommandService {
    private final SSHService sshService;
    private final MuzedoServerService muzedoServerService;
    private final InfoService infoService;
    @Autowired
    public CommandService(
        SSHService sshService,
        MuzedoServerService muzedoServerService,
        InfoService infoService
    ) {
        this.sshService = sshService;
        this.muzedoServerService = muzedoServerService;
        this.infoService = infoService;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            final var muzedoServer = muzedoServerService.createMuzedoServer("172.19.203.61");

            System.out.println(this.executeCommandShell(muzedoServer,
                "cd /opt/IBM/workdir/from_root/deploy/",
                "#"));
            System.out.println(this.executeCommandShell(muzedoServer,
                "ls",
                "#"));
            System.out.println(this.executeCommandShell(muzedoServer,
                "./wsadmin_extra.sh",
                ">"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String run(
        final RunCommandRequest runCommandRequest
    ) {
        MuzedoServer muzedoServer = muzedoServerService.getMuzedoServer(runCommandRequest.serverId());
        try {
            if (runCommandRequest.type().equals(RunCommandRequest.Type.WSADMIN)) {
                return executeCommandShell(muzedoServer, runCommandRequest.command(), ">");
            } else {
                return sshService.executeCommand(muzedoServer.clientSession, runCommandRequest.command());
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
            AtomicReference<String> currentRef = muzedoServer.getChannelShellCurrentCommand();

            if (!currentRef.compareAndSet("", currentRef.get())) {
                throw new RuntimeException("В даннный момент выполняется операция: " + currentRef.get());
            }

            infoService.writeInfo(
                muzedoServer.id,
                InfoEntry.Severity.CRIT,
                "Запущен " + command + " на " + muzedoServer.host);
            currentRef.set(command);
            String result = sshService.executeCommand(
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
