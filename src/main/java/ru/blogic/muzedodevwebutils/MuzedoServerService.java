package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.channel.ChannelShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
        } catch (Exception e) {
            log.error("#retryConnectShell exception: {}", e.getMessage(), e);
            retryConnectShell(muzedoServer);
        }
    }

    public void getServerStatus(
        final int serverId
    ) {
        MuzedoServer muzedoServer = muzedoServerDao.get(serverId);
    }
}
