package ru.blogic.muzedodevwebutils.api.file.logs;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.files.logs")
@RequiredArgsConstructor
@Getter
public class LogFileConfig {
    final List<LogFileConfigDto> servers;

    @Data
    public static class LogFileConfigDto {
        String id;
        String path;
    }
}
