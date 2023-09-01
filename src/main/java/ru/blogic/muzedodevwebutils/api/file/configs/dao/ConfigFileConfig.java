package ru.blogic.muzedodevwebutils.api.file.configs.dao;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.files.configs")
@RequiredArgsConstructor
@Getter
class ConfigFileConfig {
    private final List<ConfigFileConfigDto> list;

    @Data
    static class ConfigFileConfigDto {
        String id;
        String name;
        String path;
    }
}
