package ru.blogic.appdevwebutils.api.file.logs.config;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Внешняя конфигурация сервиса предоставления файлов логов, хранящихся на серверах приложений.
 */
@ConfigurationProperties(prefix = "app.files.logs")
@RequiredArgsConstructor
@Getter
class LogFileConfigExternalBinding {
    private final java.util.List<LogFileConfigExternalBindingDto> list;

    /**
     * Информация о файле лога.
     */
    @Data
    static class LogFileConfigExternalBindingDto {
        String id;
        String path;
    }
}
