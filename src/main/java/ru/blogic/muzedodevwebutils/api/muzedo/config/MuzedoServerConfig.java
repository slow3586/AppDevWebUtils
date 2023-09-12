package ru.blogic.muzedodevwebutils.api.muzedo.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MuzedoServerConfig {
    MuzedoServerConfigExternalBinding muzedoServerConfigExternalBinding;

    @NonFinal
    List<MuzedoServer> servers;

    @PostConstruct
    public void postConstruct() {
        this.servers = List.ofAll(
            muzedoServerConfigExternalBinding.getServers()
        ).map(dto -> new MuzedoServer(
            dto.getId(),
            dto.getHost(),
            dto.getPassword(),
            new MuzedoServer.FilePaths(
                dto.getFilePaths().getConfigs(),
                dto.getFilePaths().getLogs()
            )
        ));
    }

    public MuzedoServer get(
        final int id
    ) {
        return servers
            .find(s -> s.getId() == id)
            .getOrElseThrow(() ->
                new RuntimeException("Не найден: " + id));
    }

    public List<MuzedoServer> getAll() {
        return servers;
    }
}
