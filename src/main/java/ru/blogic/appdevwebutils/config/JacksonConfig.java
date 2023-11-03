package ru.blogic.appdevwebutils.config;

import com.fasterxml.jackson.databind.Module;
import io.vavr.jackson.datatype.VavrModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Jackson
 */
@Configuration
public class JacksonConfig {
    /**
     * Инициализирует интеграцию с Vavr.
     */
    @Bean
    Module vavrModule() {
        return new VavrModule();
    }
}
