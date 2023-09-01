package ru.blogic.muzedodevwebutils.api.muzedo;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.muzedo")
@RequiredArgsConstructor
@Getter
class MuzedoServerConfig {
    final List<MuzedoServerConfigDto> servers;

    @Data
    protected static class MuzedoServerConfigDto {
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
