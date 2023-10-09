package ru.blogic.appdevwebutils.api.app.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppServerConfig {
    final AppServerConfigExternalBinding appServerConfigExternalBinding;

    List<AppServer> servers;

    @PostConstruct
    public void postConstruct() {
        this.servers = List.ofAll(
            appServerConfigExternalBinding.getServers()
        ).map(dto -> new AppServer(
            dto.getId(),
            dto.getHost(),
            dto.getPassword(),
            new AppServer.FilePaths(
                dto.getFilePaths().getConfigs(),
                dto.getFilePaths().getLogs()
            )
        ));
    }

    public AppServer get(
        final int id
    ) {
        return servers
            .find(s -> s.getId() == id)
            .getOrElseThrow(() ->
                new RuntimeException("Не найден: " + id));
    }

    public List<AppServer> getAll() {
        return servers;
    }
}
