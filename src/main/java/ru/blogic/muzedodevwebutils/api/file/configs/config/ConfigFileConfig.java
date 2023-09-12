package ru.blogic.muzedodevwebutils.api.file.configs.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileConfig {
    ConfigFileConfigExternalBinding configFileConfigExternalBinding;

    @NonFinal
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
