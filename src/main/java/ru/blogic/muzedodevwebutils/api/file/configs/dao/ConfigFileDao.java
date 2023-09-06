package ru.blogic.muzedodevwebutils.api.file.configs.dao;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.api.file.configs.ConfigFile;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.List;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileDao {
    ConfigFileConfig configFileConfig;
    List<ConfigFile> configFiles = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        configFiles.addAll(configFileConfig.getList()
            .stream()
            .map(c -> new ConfigFile(
                c.getId(),
                c.getPath(),
                c.isSkipChangesCheck()
            )).toList()
        );
    }

    public ConfigFile get(String id) {
        return io.vavr.collection.List.ofAll(configFiles)
            .find(l -> l.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "#getConfig: Не найден: " + id));
    }

    public io.vavr.collection.List<ConfigFile> getAll() {
        return io.vavr.collection.List.ofAll(configFiles);
    }
}
