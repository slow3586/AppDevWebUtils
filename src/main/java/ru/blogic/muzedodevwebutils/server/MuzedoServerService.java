package ru.blogic.muzedodevwebutils.server;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.SSHService;
import ru.blogic.muzedodevwebutils.command.CommandDao;

import java.util.concurrent.Executors;

@Service
@Slf4j
public class MuzedoServerService {
    private final CommandDao commandDao;
    private final SSHService sshService;
    private final MuzedoServerDao muzedoServerDao;

    public MuzedoServerService(
        CommandDao commandDao,
        SSHService sshService,
        MuzedoServerDao muzedoServerDao
    ) {
        this.commandDao = commandDao;
        this.sshService = sshService;
        this.muzedoServerDao = muzedoServerDao;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            final var executorService = Executors.newCachedThreadPool();
            muzedoServerDao.getAll()
                .forEach(server -> executorService.submit(() -> {
                    server.setSshClientSession(sshService.createSession(server.host));
                    reconnectWsadminShell(server);
                }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reconnectWsadminShell(MuzedoServer muzedoServer) {
        try {
            if (muzedoServer.getWsadminShell() != null
                && !muzedoServer.getWsadminShell().isClosing()
            ) {
                muzedoServer.getWsadminShell().close();
            }

            final var shell = sshService.createShellChannel(muzedoServer.sshClientSession);

            shell.open().verify(10000);

            log.debug("{}: Starting wsadmin", muzedoServer.host);
            sshService.executeCommand(
                shell,
                commandDao.get("cd_root_deploy"));
            sshService.executeCommand(
                shell,
                commandDao.get("wsadmin_start"));
            muzedoServer.setWsadminShell(shell);
            log.debug("{}: Wsadmin started!", muzedoServer.host);
        } catch (Exception e) {
            log.error("#retryConnectShell exception: {}", e.getMessage(), e);
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                reconnectWsadminShell(muzedoServer);
            }).start();
        }
    }
}
