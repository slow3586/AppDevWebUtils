package ru.blogic.muzedodevwebutils;

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
        ClientSession session = sshService.createSession(host);
        ChannelShell channel = sshService.createShellChannel(session);

        MuzedoServer muzedoServer = new MuzedoServer(host);
        muzedoServer.setClientSession(session);
        muzedoServer.setChannelShell(channel);

        servers.add(muzedoServer);

        return muzedoServer;
    }

    public MuzedoServer getMuzedoServer(String host) {
        return servers.stream().filter(s -> s.getHost().equals(host)).findFirst()
            .orElseThrow(() -> new RuntimeException("Не найден: " + host));
    }
}
