package ru.blogic.appdevwebutils.api.info.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.info")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class InfoServiceConfigExternalBinding {
    java.util.List<ModuleConfigExternalBindingDto> modules;
    BuildTextConfigExternalBindingDto appBuildText;
    BuildTextConfigExternalBindingDto moduleBuildText;
    String dateFormat;
    String authorRegex;
    String dateRegex;
    String branchRegex;
    String hashRegex;
    String offlineText;
    String unknownBuildText;

    @Data
    static class BuildTextConfigExternalBindingDto {
        String textFormat;
        String hashLength;
        String dateTimeFormat;
    }

    @Data
    static class ModuleConfigExternalBindingDto {
        String name;
        String uri;
    }
}
