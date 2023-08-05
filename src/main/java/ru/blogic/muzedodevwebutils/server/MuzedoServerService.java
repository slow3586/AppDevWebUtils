package ru.blogic.muzedodevwebutils.server;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.SSHService;

@Service
@Slf4j
public class MuzedoServerService {
    private final SSHService sshService;
    private final MuzedoServerDao muzedoServerDao;

    public MuzedoServerService(
        SSHService sshService,
        MuzedoServerDao muzedoServerDao
    ) {
        this.sshService = sshService;
        this.muzedoServerDao = muzedoServerDao;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            muzedoServerDao.getAll()
                .parallelStream()
                .forEach(s -> {
                    final var session = sshService.createSession(s.host);
                    s.setClientSession(session);
                    reconnectWsadminShell(s);
                });
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

            final var shell = sshService.createShellChannel(muzedoServer.clientSession);
            muzedoServer.setWsadminShell(shell);

            shell.open().verify(10000);

            log.debug("{}: Starting wsadmin", muzedoServer.host);
            sshService.executeCommand(
                shell,
                "cd /root/deploy/",
                "#");
            sshService.executeCommand(
                shell,
                "./wsadmin_extra.sh",
                ">");
            log.debug("{}: Wsadmin started!", muzedoServer.host);
        } catch (Exception e) {
            log.error("#retryConnectShell exception: {}", e.getMessage(), e);
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                reconnectWsadminShell(muzedoServer);
            }).start();
        }
    }
}
