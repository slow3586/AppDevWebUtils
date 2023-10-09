package ru.blogic.appdevwebutils.api.file.configs.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.files.configs")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class ConfigFileConfigExternalBinding {
    java.util.List<ConfigFileConfigExternalBindingDto> list;

    @Data
    static class ConfigFileConfigExternalBindingDto {
        String id;
        String path;
    }
}
