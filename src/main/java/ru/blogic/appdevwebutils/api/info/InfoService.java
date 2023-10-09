package ru.blogic.appdevwebutils.api.info;

import io.vavr.collection.List;
import io.vavr.control.Option;
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

import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoService {
    AppServerConfig appServerConfig;
    InfoServiceConfig infoServiceConfig;
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
                        moduleBuildInfo)
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
                                    infoServiceConfig.getAuthorPattern()
                                        .matcher(answer)
                                        .group(),
                                    (Instant) (infoServiceConfig.getDatePattern()
                                        .matcher(answer)
                                        .group()
                                        .transform(infoServiceConfig.getDateTimeFormat()::parse)),
                                    infoServiceConfig.getBranchPattern()
                                        .matcher(answer)
                                        .group(),
                                    infoServiceConfig.getHashPattern()
                                        .matcher(answer)
                                        .group()))
                            .doOnError((err) -> {
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
                                appBuildInfo))
                            .getOrNull());
                    }).doOnError(e -> server.setAppBuildText(null))
                    .subscribe());
    }

    private String formatBuildText(String textFormat, AppServer.ModuleBuildInfo moduleBuildInfo) {
        return textFormat
            .replaceAll("\\$author",
                StringUtils.defaultString(moduleBuildInfo.author(), "<?>"))
            .replaceAll("\\$date",
                Option.of(moduleBuildInfo.date())
                    .map(date -> infoServiceConfig.getModuleBuildTextConfig()
                        .dateTimeFormat()
                        .format(date)
                    ).getOrElse("<?>"))
            .replaceAll("\\$branch",
                StringUtils.defaultString(moduleBuildInfo.branch(), "<?>"))
            .replaceAll("\\$hash",
                StringUtils.substring(StringUtils.defaultString(moduleBuildInfo.hash(), "<?>"),
                    0,
                    infoServiceConfig.getModuleBuildTextConfig().hashLength()));
    }

    @CacheEvict(allEntries = true, value = "getServerInfo")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerInfoCache() {}
}
