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
            muzedoServerDao.getAll().forEach(s -> {
                final var session = sshService.createSession(s.host);
                s.setClientSession(session);
                retryConnectShell(s);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void retryConnectShell(MuzedoServer muzedoServer) {
        try {
            if (muzedoServer.getChannelShell() == null) {
                muzedoServer.setChannelShell(sshService.createShellChannel(muzedoServer.clientSession));
            }
            final var channel = muzedoServer.getChannelShell();
            channel.open().verify(10000);
            channel.addCloseFutureListener(future -> {
                log.warn("ShellChannel closed: {}", muzedoServer.host);
                retryConnectShell(muzedoServer);
            });

            log.debug("{}: Starting wsadmin", muzedoServer.host);
            sshService.executeCommand(
                channel,
                "cd /root/deploy/",
                "#");
            sshService.executeCommand(
                channel,
                "./wsadmin_extra.sh",
                ">");
            log.debug("{}: Wsadmin started!", muzedoServer.host);
        } catch (Exception e) {
            log.error("#retryConnectShell exception: {}", e.getMessage(), e);
            retryConnectShell(muzedoServer);
        }
    }
}
