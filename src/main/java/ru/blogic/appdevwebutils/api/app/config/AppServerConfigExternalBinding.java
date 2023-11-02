package ru.blogic.appdevwebutils.api.app.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Внешняя конфигурация, хранящая информацию о серверах приложений
 */
@ConfigurationProperties(prefix = "app.app")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
class AppServerConfigExternalBinding {
    java.util.List<AppServerConfigDto> servers;

    @Data
    protected static class AppServerConfigDto {
        int id;
        String host;
        String password;
        FilePaths filePaths;

        @Data
        protected static class FilePaths {
            String configs;
            String logs;
        }
    }
}
