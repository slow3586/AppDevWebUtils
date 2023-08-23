package ru.blogic.muzedodevwebutils.api.muzedo;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.List;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MuzedoServerDao {
    MuzedoServerConfig muzedoServerConfig;
    List<MuzedoServer> servers = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        this.servers.addAll(
            muzedoServerConfig.getServers()
                .stream()
                .map(dto -> new MuzedoServer(
                    dto.getId(),
                    dto.getHost(),
                    dto.getPassword()
                ))
                .toList());
    }

    public MuzedoServer get(
        final int id
    ) {
        return this.getAll()
            .find(s -> s.getId() == id)
            .getOrElseThrow(() ->
                new RuntimeException("Не найден: " + id));
    }

    public io.vavr.collection.List<MuzedoServer> getAll() {
        return io.vavr.collection.List.ofAll(servers);
    }
}
