package ru.blogic.appdevwebutils.api.file.logs.config;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.files.logs")
@RequiredArgsConstructor
@Getter
class LogFileConfigExternalBinding {
    private final java.util.List<LogFileConfigExternalBindingDto> list;

    @Data
    static class LogFileConfigExternalBindingDto {
        String id;
        String path;
    }
}
