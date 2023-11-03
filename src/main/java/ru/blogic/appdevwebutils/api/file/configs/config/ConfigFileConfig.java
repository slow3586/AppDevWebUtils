package ru.blogic.appdevwebutils.api.file.configs.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

/**
 * Конфигурация сервиса загрузки/сохранения файлов конфигурации на сервере приложения.
 */
@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class ConfigFileConfig {
    /**
     * Внешняя конфигурация
     */
    final ConfigFileConfigExternalBinding configFileConfigExternalBinding;

    List<ConfigFile> configFiles;

    /**
     * Конвертация внешней конфигурации.
     */
    @PostConstruct
    public void postConstruct() {
        configFiles = List.ofAll(configFileConfigExternalBinding.getList())
            .map(configEntry ->
                new ConfigFile(
                    configEntry.getId(),
                    configEntry.getPath()));
    }

    /**
     * Предоставляет информацию о файле конфигурации с указанным ID.
     */
    public ConfigFile get(String id) {
        return configFiles
            .find(l -> l.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "#getConfig: Не найден: " + id));
    }
}
