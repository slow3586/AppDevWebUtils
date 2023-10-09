package ru.blogic.appdevwebutils.api.file.configs.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfigFileConfig {
    final ConfigFileConfigExternalBinding configFileConfigExternalBinding;

    List<ConfigFile> configFiles;

    @PostConstruct
    public void postConstruct() {
        configFiles = List.ofAll(configFileConfigExternalBinding.getList())
            .map(configEntry ->
                new ConfigFile(
                    configEntry.getId(),
                    configEntry.getPath()));
    }

    public ConfigFile get(String id) {
        return configFiles
            .find(l -> l.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "#getConfig: Не найден: " + id));
    }

    public List<ConfigFile> getAll() {
        return configFiles;
    }
}
