package ru.blogic.appdevwebutils.api.info;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
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
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.api.info.config.InfoServiceConfig;
import ru.blogic.appdevwebutils.api.info.dto.GetServerInfoResponse;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoService {
    AppServerConfig appServerConfig;
    InfoServiceConfig infoServiceConfig;
    static String UNKNOWN = "<?>";
    static WebClient WEB_CLIENT = WebClient.create();

    @PostConstruct
    public void postConstruct() {
        try {
            Hooks.onErrorDropped(error -> {
                if (!StringUtils.containsAnyIgnoreCase(error.getMessage(),
                    "503 Service Unavailable",
                    "Connection refused: no further information")) {
                    log.error("Hooks.onErrorDropped: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DisableLoggingAspect
    @Cacheable(value = "getServerInfo")
    public GetServerInfoResponse getServerInfo(int serverId) {
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
                    moduleBuildInfo.online()
                        ? this.formatBuildText(
                        infoServiceConfig.getModuleBuildTextConfig().textFormat(),
                        moduleBuildInfo,
                        infoServiceConfig.getModuleBuildTextConfig().dateTimeFormat())
                        : null)));
    }

    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void updateInfo() {
        appServerConfig
            .getAll()
            .forEach(server ->
                Flux.fromStream(infoServiceConfig.getModuleConfigs().toJavaStream())
                    .flatMapSequential(moduleConfig ->
                        WEB_CLIENT.get()
                            .uri("http://" + server.getHost() + "/" + moduleConfig.uri())
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnError(e1 -> {
                                if (!StringUtils.containsAnyIgnoreCase(e1.getMessage(),
                                    "503 Service Unavailable",
                                    "Connection refused: no further information")
                                ) {
                                    log.error("#updateInfo {} {}",
                                        server.getId(),
                                        e1.getMessage());
                                }
                            }).map(answer ->
                                new AppServer.ModuleBuildInfo(
                                    moduleConfig.name(),
                                    true,
                                    this.find(answer, infoServiceConfig.getAuthorPattern()),
                                    Option.of(this.find(
                                            answer,
                                            infoServiceConfig.getDatePattern()))
                                        .filter(StringUtils::isNotBlank)
                                        .flatMap(dateStr -> Try.of(() ->
                                                infoServiceConfig.getDateTimeFormat().parse(dateStr)
                                            ).onFailure((err) -> log.error("#updateInfo date: " + err.getMessage()))
                                            .toOption())
                                        .map(ZonedDateTime::from)
                                        .getOrNull(),
                                    this.find(answer, infoServiceConfig.getBranchPattern()),
                                    this.find(answer, infoServiceConfig.getHashPattern()))
                            ).doOnError((err) -> {
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
                                    null,
                                    null,
                                    null,
                                    null)))
                    ).collectList()
                    .mapNotNull(List::ofAll)
                    .doOnSuccess(appBuildInfoList -> {
                        server.setModuleBuildInfoList(appBuildInfoList);
                        server.setAppBuildText(appBuildInfoList
                            .filter(appBuildInfo -> appBuildInfo.date() != null)
                            .minBy(Comparator.comparing(AppServer.ModuleBuildInfo::date))
                            .map(appBuildInfo -> this.formatBuildText(
                                infoServiceConfig.getAppBuildTextConfig().textFormat(),
                                appBuildInfo,
                                infoServiceConfig.getAppBuildTextConfig().dateTimeFormat()))
                            .getOrNull());
                    }).doOnError(e -> server.setAppBuildText(null))
                    .subscribe());
    }

    private String formatBuildText(
        final String textFormat,
        final AppServer.ModuleBuildInfo moduleBuildInfo,
        final DateTimeFormatter dateTimeFormatter
        ) {
        return textFormat
            .replaceAll("\\$author",
                Option.of(moduleBuildInfo.author())
                    .filter(StringUtils::isNotBlank)
                    .getOrElse(UNKNOWN))
            .replaceAll("\\$date",
                Option.of(moduleBuildInfo.date())
                    .map(dateTimeFormatter::format)
                    .getOrElse(UNKNOWN))
            .replaceAll("\\$branch",
                Option.of(moduleBuildInfo.branch())
                    .filter(StringUtils::isNotBlank)
                    .getOrElse(UNKNOWN))
            .replaceAll("\\$hash",
                Option.of(moduleBuildInfo.hash())
                    .filter(StringUtils::isNotBlank)
                    .map(s -> s.substring(0, infoServiceConfig.getModuleBuildTextConfig().hashLength()))
                    .getOrElse(UNKNOWN));
    }

    private String find(String text, Pattern pattern) {
        return Option.of(text)
            .filter(StringUtils::isNotBlank)
            .map(pattern::matcher)
            .filter(Matcher::find)
            .map(m -> m.group(1))
            .filter(s -> !StringUtils.containsAnyIgnoreCase(s, "fatal:"))
            .getOrNull();
    }

    @CacheEvict(allEntries = true, value = "getServerInfo")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerInfoCache() {}
}
