package ru.blogic.appdevwebutils.api.info.config;

import io.vavr.collection.List;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Конфигурация сервиса предоставления информации о сервере приложения.
 */
@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@Getter
public class InfoServiceConfig {
    /** Внешняя конфигурация. */
    final InfoServiceConfigExternalBinding infoServiceConfigExternalBinding;

    List<ModuleConfig> moduleConfigs;
    BuildTextConfig moduleBuildTextConfig;
    BuildTextConfig appBuildTextConfig;
    Pattern authorPattern;
    Pattern datePattern;
    Pattern branchPattern;
    Pattern hashPattern;
    DateTimeFormatter dateTimeFormat;
    String unknownValueText;
    String unknownBuildText;
    boolean useHttps;
    String offlineText;

    /** Конвертация внешней конфигурации. */
    @PostConstruct
    public void postConstruct() {
        moduleConfigs = List.ofAll(
            infoServiceConfigExternalBinding.getModules()
        ).map(configEntry ->
            new ModuleConfig(
                configEntry.getName(),
                configEntry.getUri()));
        authorPattern = Pattern.compile(infoServiceConfigExternalBinding.getAuthorRegex());
        datePattern = Pattern.compile(infoServiceConfigExternalBinding.getDateRegex());
        branchPattern = Pattern.compile(infoServiceConfigExternalBinding.getBranchRegex());
        hashPattern = Pattern.compile(infoServiceConfigExternalBinding.getHashRegex());
        appBuildTextConfig = new BuildTextConfig(
            infoServiceConfigExternalBinding.getAppBuildText().textFormat,
            Try.of(() ->
                    Integer.parseInt(infoServiceConfigExternalBinding.getAppBuildText().hashLength))
                .getOrElse(6),
            DateTimeFormatter.ofPattern(infoServiceConfigExternalBinding.getAppBuildText().dateTimeFormat));
        moduleBuildTextConfig = new BuildTextConfig(
            infoServiceConfigExternalBinding.getModuleBuildText().textFormat,
            Try.of(() ->
                    Integer.parseInt(infoServiceConfigExternalBinding.getModuleBuildText().hashLength))
                .getOrElse(6),
            DateTimeFormatter.ofPattern(infoServiceConfigExternalBinding.getModuleBuildText().dateTimeFormat));
        dateTimeFormat = DateTimeFormatter.ofPattern(
            infoServiceConfigExternalBinding.getDateFormat(), Locale.ENGLISH);
        unknownValueText = infoServiceConfigExternalBinding.getUnknownValueText();
        useHttps = infoServiceConfigExternalBinding.isUseHttps();
        unknownBuildText = infoServiceConfigExternalBinding.getUnknownBuildText();
        offlineText = infoServiceConfigExternalBinding.getOfflineText();
    }

    /**
     * Конфигурация информации о сборке.
     * @param textFormat Формат текста сборки.
     * @param hashLength Длина хэша.
     * @param dateTimeFormat Формат даты.
     */
    public record BuildTextConfig(
        String textFormat,
        int hashLength,
        DateTimeFormatter dateTimeFormat
    ) {}

    /**
     * Конфигурация информации о модуле.
     * @param name Название модуля.
     * @param uri URI для получения внешней информации о сборке модуля.
     */
    public record ModuleConfig(
        String name,
        String uri
    ) {}
}
