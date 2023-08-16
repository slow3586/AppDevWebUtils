package ru.blogic.muzedodevwebutils.api.file.configs;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.files.configs")
@RequiredArgsConstructor
@Getter
public class ConfigFileConfig {
    final List<ConfigFileConfigDto> servers;

    @Data
    public static class ConfigFileConfigDto {
        String id;
        String path;
    }
}
