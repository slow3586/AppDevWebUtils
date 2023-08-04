package ru.blogic.muzedodevwebutils.server;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MuzedoServerDao {
    private final List<MuzedoServer> servers = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        servers.add(new MuzedoServer(60, "172.19.203.60"));
        servers.add(new MuzedoServer(61, "172.19.203.61"));
    }

    public MuzedoServer create(MuzedoServer muzedoServer) {
        servers.add(muzedoServer);
        return muzedoServer;
    }

    public MuzedoServer get(
        final int id
    ) {
        return servers
            .stream()
            .filter(s -> s.getId() == id)
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException("Не найден: " + id));
    }

    public List<MuzedoServer> getAll() {
        return new ArrayList<>(servers);
    }
}
