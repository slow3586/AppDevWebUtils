package ru.blogic.appdevwebutils.api.info;

import io.vavr.Function1;
import io.vavr.Predicates;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.api.info.config.InfoServiceConfig;
import ru.blogic.appdevwebutils.api.info.dto.GetServerInfoResponse;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;
import ru.blogic.appdevwebutils.utils.Utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис, отвечающий за регулярное обновление актуальной информации о сборках, стоящих на
 * серверах приложений, хранение этой информации,
 * а так же за получение этой информации из интерфейса пользователя.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoService {
    AppServerConfig appServerConfig;
    InfoServiceConfig infoServiceConfig;
    WebClient.Builder webClientBuilder;

    /**
     * Предоставляет информацию о сборке приложения на указанном сервере.
     */
    @DisableLoggingAspect
    @Cacheable(value = "getServerInfo")
    public GetServerInfoResponse getServerInfo(
        final int serverId
    ) {
        final AppServer appServer = appServerConfig.get(serverId);

        return new GetServerInfoResponse(
            appServer.getSshClientSession() != null
                && appServer.getSshClientSession().isOpen()
                && appServer.getWsadminShell() != null
                && appServer.getWsadminShell().isOpen()
                && !Option.of(appServer.getExecutingCommand())
                .exists(Command::blocksWsadmin),
            Option.of(appServer.getScheduledCommand())
                .map(AppServer.ScheduledCommand::command)
                .getOrNull(),
            appServer.getExecutingCommand(),
            appServer.getExecutingCommandTimer().getTime(),
            Option.of(appServer.getScheduledCommand())
                .map(AppServer.ScheduledCommand::scheduledFuture)
                .map(f -> f.getDelay(TimeUnit.SECONDS))
                .map(Math::toIntExact)
                .getOrElse(0),
            appServer.getAppBuildText(),
            appServer.getModuleBuildInfoList()
                .map(moduleBuildInfo -> new GetServerInfoResponse.ModuleBuildInfo(
                    moduleBuildInfo.name(),
                    this.formatBuildText(
                        infoServiceConfig.getModuleBuildTextConfig(),
                        moduleBuildInfo))));
    }

    /**
     * Отвечает за регулярное получение актуальной информации о сборках приложений на серверах.
     */
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void updateInfo() {
        appServerConfig
            .getAll()
            .forEach(server ->
                Flux.fromStream(infoServiceConfig.getModuleConfigs().toJavaStream())
                    // Собираем внешнюю информацию о сборке каждого модуля отдельного сервера приложения
                    .flatMapSequential(moduleConfig ->
                        webClientBuilder
                            .baseUrl("http"
                                + (infoServiceConfig.isUseHttps() ? "s" : "")
                                + "://" + server.getHost()
                                + "/"
                                + moduleConfig.uri())
                            .build()
                            .get()
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(answer -> {
                                final Function1<Pattern, String> find =
                                    (pattern) -> Option.of(answer)
                                        .filter(StringUtils::isNotBlank)
                                        .map(pattern::matcher)
                                        .filter(Matcher::find)
                                        .map(m -> m.group(1))
                                        .filter(s -> !StringUtils.containsAnyIgnoreCase(s, "fatal:"))
                                        .filter(StringUtils::isNotBlank)
                                        .getOrNull();

                                return new AppServer.ModuleBuildInfo(
                                    moduleConfig.name(),
                                    StringUtils.isNotBlank(answer) && !answer.equals("None"),
                                    true,
                                    find.apply(infoServiceConfig.getAuthorPattern()),
                                    Option.of(find.apply(infoServiceConfig.getDatePattern()))
                                        .flatMap(dateStr -> Try.of(() ->
                                                infoServiceConfig.getDateTimeFormat().parse(dateStr)
                                            ).onFailure((err) -> log.error("#updateInfo date: " + err.getMessage()))
                                            .toOption())
                                        .map(ZonedDateTime::from)
                                        .getOrNull(),
                                    find.apply(infoServiceConfig.getBranchPattern()),
                                    find.apply(infoServiceConfig.getHashPattern()));
                            }).doOnError((err) -> {
                                if (!StringUtils.containsAnyIgnoreCase(err.getMessage(),
                                    "503 Service Unavailable",
                                    "Connection refused: no further information")) {
                                    log.error("#updateInfo {} {}",
                                        server.getId(),
                                        err.getMessage());
                                }
                            }).onErrorResume((err) ->
                                Mono.just(new AppServer.ModuleBuildInfo(
                                    moduleConfig.name(),
                                    false,
                                    false,
                                    null,
                                    null,
                                    null,
                                    null)))
                    ).collectList()
                    // Информация об отдельных модулях собрана, формируем общую информацию о сборке
                    .mapNotNull(List::ofAll)
                    .doOnSuccess(appBuildInfoList -> {
                        server.setModuleBuildInfoList(appBuildInfoList);
                        server.setAppBuildText(
                            appBuildInfoList
                                .filter(AppServer.ModuleBuildInfo::online)
                                .isEmpty()
                                ? infoServiceConfig.getOfflineText()
                                : appBuildInfoList
                                    .filter(Predicates.allOf(
                                        AppServer.ModuleBuildInfo::online,
                                        AppServer.ModuleBuildInfo::hasBuildInfo))
                                    .minBy(Comparator.comparing(
                                        moduleBuildInfo -> Option.of(moduleBuildInfo.date())
                                            .getOrElse(Utils::getZeroDate)))
                                    .map(appBuildInfo -> this.formatBuildText(
                                        infoServiceConfig.getAppBuildTextConfig(),
                                        appBuildInfo))
                                    .getOrElse(infoServiceConfig.getUnknownBuildText()));
                    }).doOnError(e -> server.setAppBuildText(infoServiceConfig.getOfflineText()))
                    .subscribe());
    }

    /**
     * Конвертирует ModuleBuildInfo в текст с информацией о сборке для пользователя согласно указанному шаблону.
     * Учитывает, включен ли модуль и есть ли по нему внешняя информация о сборке.
     */
    private String formatBuildText(
        final InfoServiceConfig.BuildTextConfig buildTextConfig,
        final AppServer.ModuleBuildInfo moduleBuildInfo
    ) {
        final String result;
        if (!moduleBuildInfo.online()) {
            result = infoServiceConfig.getOfflineText();
        } else if (!moduleBuildInfo.hasBuildInfo()) {
            result = infoServiceConfig.getUnknownBuildText();
        } else {
            final Function1<String, String> replaceText = (text) -> Option.of(text)
                .filter(StringUtils::isNotBlank)
                .getOrElse(infoServiceConfig.getUnknownValueText());

            result = buildTextConfig.textFormat()
                .replaceAll("\\$author", replaceText.apply(moduleBuildInfo.author()))
                .replaceAll("\\$date",
                    Option.of(moduleBuildInfo.date())
                        .map(buildTextConfig.dateTimeFormat()::format)
                        .getOrElse(infoServiceConfig.getUnknownValueText()))
                .replaceAll("\\$branch", replaceText.apply(moduleBuildInfo.branch()))
                .replaceAll("\\$hash",
                    StringUtils.substring(
                        replaceText.apply(moduleBuildInfo.hash()),
                        0,
                        buildTextConfig.hashLength()));
        }
        return result;
    }

    /**
     * Отвечает за очистку кэша с информацией о сборках на стенде.
     */
    @CacheEvict(allEntries = true, value = "getServerInfo")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerInfoCache() {}
}
