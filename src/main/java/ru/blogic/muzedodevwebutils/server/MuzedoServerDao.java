package ru.blogic.muzedodevwebutils.server;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.List;

@Repository
@DisableLoggingAspect
public class MuzedoServerDao {
    private final List<MuzedoServer> servers = new ArrayList<>();

    @Value("${app.p0}")
    private String p0;

    @Value("${app.p1}")
    private String p1;

    @PostConstruct
    public void postConstruct() {
        servers.add(new MuzedoServer(58,
            "172.19.203.58",
            "http://172.19.203.58",
            p0));
        servers.add(new MuzedoServer(59,
            "172.19.203.59",
            "http://172.19.203.59",
            p0));
        servers.add(new MuzedoServer(60,
            "172.19.203.60",
            "http://172.19.203.60",
            p0));
        servers.add(new MuzedoServer(61,
            "172.19.203.61",
            "http://172.19.203.61",
            p0));
        servers.add(new MuzedoServer(146,
            "172.19.203.146",
            "http://172.19.203.146",
            p1));
        servers.add(new MuzedoServer(147,
            "172.19.203.147",
            "http://172.19.203.147",
            p1));
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
