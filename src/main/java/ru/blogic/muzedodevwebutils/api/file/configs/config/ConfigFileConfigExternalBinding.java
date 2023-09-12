package ru.blogic.muzedodevwebutils.api.file.configs.config;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.files.configs")
@RequiredArgsConstructor
@Getter
class ConfigFileConfigExternalBinding {
    private final java.util.List<ConfigFileConfigExternalBindingDto> list;

    @Data
    static class ConfigFileConfigExternalBindingDto {
        String id;
        String path;
    }
}
