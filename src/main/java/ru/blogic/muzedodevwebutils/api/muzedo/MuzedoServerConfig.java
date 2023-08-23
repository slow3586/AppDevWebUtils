package ru.blogic.muzedodevwebutils.api.muzedo;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.muzedo")
@RequiredArgsConstructor
@Getter
public class MuzedoServerConfig {
    final List<MuzedoServerConfigDto> servers;

    @Data
    public static class MuzedoServerConfigDto {
        int id;
        String host;
        String password;
    }
}
