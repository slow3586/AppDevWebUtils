package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class CommandService {
    private final SSHService sshService;
    private final MuzedoServerService muzedoServerService;

    @Autowired
    public CommandService(
        SSHService sshService,
        MuzedoServerService muzedoServerService
    ) {
        this.sshService = sshService;
        this.muzedoServerService = muzedoServerService;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            MuzedoServer muzedoServer = muzedoServerService.createMuzedoServer("172.19.203.61");

            System.out.println(this.executeCommandShell(muzedoServer,
                "cd /opt/IBM/workdir/from_root/deploy/",
                "#"));
            System.out.println(this.executeCommandShell(muzedoServer,
                "ls",
                "#"));
            System.out.println(this.executeCommandWsadmin(muzedoServer,
                "./wsadmin_extra.sh"));
            System.out.println(this.executeCommandWsadmin(muzedoServer,
              "cnf()"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String run(
        RunCommandRequest runCommandRequest
    ) {
        MuzedoServer muzedoServer = muzedoServerService.getMuzedoServer(runCommandRequest.host());
        if (runCommandRequest.type().equals(RunCommandRequest.Type.WSADMIN)) {
            return this.executeCommandWsadmin(muzedoServer, runCommandRequest.command());
        } else {
            return this.executeCommandSsh(muzedoServer, runCommandRequest.command());
        }
    }

    public String executeCommandShell(
        MuzedoServer muzedoServer,
        String command,
        String readySymbol
    ) {
        try {
            AtomicReference<String> currentRef = muzedoServer.getChannelShellCurrentCommand();

            if (!currentRef.compareAndSet("", currentRef.get())) {
                throw new RuntimeException("В даннный момент выполняется операция: " + currentRef.get());
            }

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

    public String executeCommandWsadmin(
        MuzedoServer muzedoServer,
        String command
    ) {
        try {
            return executeCommandShell(muzedoServer, command, ">");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommandSsh(
        MuzedoServer muzedoServer,
        String command
    ) {
        try {
            return sshService.executeCommand(muzedoServer.clientSession, command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
