package ru.blogic.muzedodevwebutils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MuzedoServerService {
    private final SSHService sshService;
    private final List<MuzedoServer> servers = new ArrayList<>();

    public MuzedoServerService(
        SSHService sshService
    ) {
        this.sshService = sshService;
    }

    public MuzedoServer createMuzedoServer(String host) {
        final var session = sshService.createSession(host);
        final var channel = sshService.createShellChannel(session);

        final var muzedoServer = new MuzedoServer(
            Integer.parseInt(StringUtils.substringAfterLast(host, ".")),
            host);
        muzedoServer.setClientSession(session);
        muzedoServer.setChannelShell(channel);

        servers.add(muzedoServer);

        return muzedoServer;
    }

    public MuzedoServer getMuzedoServer(
        final int id
    ) {
        return servers
            .stream()
            .filter(s -> s.getId() == id)
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException("Не найден: " + id));
    }
}
