package ru.blogic.muzedodevwebutils.api.file.logs.dao;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.files.logs")
@RequiredArgsConstructor
@Getter
class LogFileConfig {
    private final List<LogFileConfigDto> list;

    @Data
    static class LogFileConfigDto {
        String id;
        String path;
    }
}
