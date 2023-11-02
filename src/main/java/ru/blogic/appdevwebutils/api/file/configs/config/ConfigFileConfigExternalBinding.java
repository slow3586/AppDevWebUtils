package ru.blogic.appdevwebutils.api.file.configs.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Внешняя конфигурация сервиса загрузки/сохранения файлов конфигурации на сервере приложения.
 */
@ConfigurationProperties(prefix = "app.files.configs")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
class ConfigFileConfigExternalBinding {
    java.util.List<ConfigFileConfigExternalBindingDto> list;

    /**
     * Информация о файле конфигурации.
     */
    @Data
    static class ConfigFileConfigExternalBindingDto {
        String id;
        String path;
    }
}
