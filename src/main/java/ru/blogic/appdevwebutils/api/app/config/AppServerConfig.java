package ru.blogic.appdevwebutils.api.app.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

/**
 * Конфигурация, хранящая информацию о серверах приложений
 */
@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED)
public class AppServerConfig {
    /**
     * Внешняя конфигурация
     */
    final AppServerConfigExternalBinding appServerConfigExternalBinding;

    List<AppServer> servers;

    /**
     * Конвертация внешней конфигурации.
     */
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
