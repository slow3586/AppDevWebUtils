package ru.blogic.appdevwebutils.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Настройка Reactor Hooks
 */
@Slf4j
@Configuration
public class ReactorHooksConfig {
    @PostConstruct
    public void registerOnErrorDropped(){
        Hooks.onErrorDropped(error -> {
            if (!StringUtils.containsAnyIgnoreCase(error.getMessage(),
                "503 Service Unavailable",
                "Connection refused: no further information")) {
                log.error("Hooks.onErrorDropped: " + error.getMessage());
            }
        });
    }
}
