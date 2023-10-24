package ru.blogic.appdevwebutils.api.info.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация сервиса предоставления информации о стендах приложений.
 */
@ConfigurationProperties(prefix = "app.info")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class InfoServiceConfigExternalBinding {
    /**
     * Конфигурация информации о модулях приложения.
     */
    java.util.List<ModuleConfigExternalBindingDto> modules;
    /**
     * Конфигурация информации о сборке приложения.
     */
    BuildTextConfigExternalBindingDto appBuildText;
    /**
     * Конфигурация информация о сборке модуля приложения.
     */
    BuildTextConfigExternalBindingDto moduleBuildText;
    /**
     * Формат даты во внешней информации о сборке приложения.
     */
    String dateFormat;
    /**
     * Шаблон автора сборки во внешней информации.
     */
    String authorRegex;
    /**
     * Шаблон даты сборки во внешней информации.
     */
    String dateRegex;
    /**
     * Шаблон ветки сборки во внешней информации.
     */
    String branchRegex;
    /**
     * Шаблон хэша сборки во внешней информации.
     */
    String hashRegex;
    /**
     * Текст, если внешней информации о сборке нет.
     */
    String unknownBuildText;
    /**
     * Текст, если значение по шаблону во внешней сборке не найдено.
     */
    String unknownValueText;
    /**
     * Нужно ли использовать HTTPS в запросах внешней информации о сборках.
     */
    boolean useHttps;
    /**
     * Текст, если модуль не включен.
     */
    String offlineText;

    /**
     * Конфигурация формата информации о сборке.
     */
    @Data
    final static class BuildTextConfigExternalBindingDto {
        /**
         * Формат текста информации о сборке.
         */
        String textFormat;
        /**
         * Длина хэша.
         */
        String hashLength;
        /**
         * Формат даты.
         */
        String dateTimeFormat;
    }

    /**
     * Конфигурация формата инфорации о сборке модуля.
     */
    @Data
    final static class ModuleConfigExternalBindingDto {
        /**
         * Название модуля.
         */
        String name;
        /**
         * URI для получения внешней информации о сборке модуля.
         */
        String uri;
    }
}
