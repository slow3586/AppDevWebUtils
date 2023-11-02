package ru.blogic.appdevwebutils.api.app.ssh.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация, хранящая информацию о SSH соединении
 */
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.ssh-service")
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class SshServiceConfig {
    String username;
    String port;
    String keyFile;
    String keyPw;
}
