package ru.blogic.muzedodevwebutils.api.info;

import io.vavr.Function2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.info.dto.GetServerInfoResponse;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.config.MuzedoServerConfig;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoService {
    MuzedoServerConfig muzedoServerConfig;
    static DateTimeFormatter DATE_FORMAT_MUZEDO_BUILD_INFO = DateTimeFormatter.ofPattern(
        "HH:mm:ss dd.MM.yyyy z Z", Locale.ENGLISH);
    static DateTimeFormatter DATE_FORMAT_APP_BUILD_INFO = DateTimeFormatter.ofPattern(
        "dd.MM.yy_HH.mm", Locale.ENGLISH);
    static WebClient WEB_CLIENT = WebClient.create();
    static String GP_BUILD_INFO_URI = "UZDO/api/app/buildInfo";
    static String INTEG_BUILD_INFO_URI = "UZDO-ui/rest/app/buildInfo";
    static String UNKNOWN_BUILD = "Неизвестная сборка";

    @NonFinal
    @Value("${app.buildVersion:}")
    String buildVersion;

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
    //@Cacheable(value = "getServerInfo")
    public GetServerInfoResponse getServerInfo(int serverId) {
        final MuzedoServer muzedoServer = muzedoServerConfig.get(serverId);

        return new GetServerInfoResponse(
            muzedoServer.getSshClientSession() != null
                && muzedoServer.getSshClientSession().isOpen()
                && muzedoServer.getWsadminShell() != null
                && muzedoServer.getWsadminShell().isOpen()
                && !Option.of(muzedoServer.getExecutingCommand())
                .exists(Command::blocksWsadmin),
            Option.of(muzedoServer.getScheduledCommand())
                .map(MuzedoServer.ScheduledCommand::command)
                .getOrNull(),
            muzedoServer.getExecutingCommand(),
            muzedoServer.getExecutingCommandTimer().getTime(),
            Option.of(muzedoServer.getScheduledCommand())
                .map(MuzedoServer.ScheduledCommand::scheduledFuture)
                .map(f -> f.getDelay(TimeUnit.SECONDS))
                .map(Math::toIntExact)
                .getOrElse(0),
            muzedoServer.getBuild(),
            muzedoServer.getGpBuildInfo(),
            muzedoServer.getIntegBuildInfo()
        );
    }

    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void updateInfo() {
        muzedoServerConfig
            .getAll()
            .forEach(server -> {
                final Function2<String, Consumer<MuzedoServer.MuzedoBuildInfo>, Mono<Void>>
                    getBuildInfo = (uri, setBuildInfo) ->
                    WEB_CLIENT.get()
                        .uri("http://" + server.getHost() + "/" + uri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(e -> {
                            server.setGpBuildInfo(null);
                            server.setIntegBuildInfo(null);
                            if (!StringUtils.containsAnyIgnoreCase(e.getMessage(),
                                "503 Service Unavailable",
                                "Connection refused: no further information")) {
                                log.error("#updateInfo {} {}",
                                    server.getId(),
                                    e.getMessage());
                            }
                        }).doOnSuccess(answer -> {
                            final List<String> lines = Arrays.stream(
                                    StringUtils.split(
                                        StringUtils.defaultString(answer),
                                        "\n"))
                                .map(l -> StringUtils
                                    .substringAfter(l, ":").trim())
                                .toList();
                            setBuildInfo.accept((lines.size() < 4)
                                ? new MuzedoServer.MuzedoBuildInfo(null,
                                null,
                                null,
                                null)
                                : new MuzedoServer.MuzedoBuildInfo(lines.get(1),
                                    lines.get(2),
                                    lines.get(3),
                                    StringUtils.substring(lines.get(4), 0, 5)));
                        }).then();

                getBuildInfo.apply(GP_BUILD_INFO_URI, server::setGpBuildInfo)
                    .and(getBuildInfo.apply(INTEG_BUILD_INFO_URI, server::setIntegBuildInfo))
                    .then()
                    .doOnSuccess(a -> {
                        final MuzedoServer.MuzedoBuildInfo gpBuildInfo = server.getGpBuildInfo();
                        final MuzedoServer.MuzedoBuildInfo integBuildInfo = server.getIntegBuildInfo();
                        if (gpBuildInfo != null && gpBuildInfo.date() != null
                            && integBuildInfo != null && integBuildInfo.date() != null
                        ) {
                            final MuzedoServer.MuzedoBuildInfo buildInfo = (gpBuildInfo.date().compareTo(integBuildInfo.date()) > 0)
                                ? gpBuildInfo
                                : integBuildInfo;
                            server.setBuild(Option.of(this.buildVersion)
                                .filter(StringUtils::isNotBlank)
                                .getOrElse(StringUtils.substringAfterLast(integBuildInfo.branch(), "/"))
                                + "_" +
                                Try.of(() ->
                                    DATE_FORMAT_APP_BUILD_INFO.format(
                                        DATE_FORMAT_MUZEDO_BUILD_INFO.parse(buildInfo.date())
                                    )
                                ).getOrNull());
                        } else {
                            server.setBuild(UNKNOWN_BUILD);
                        }
                    }).doOnError(e -> server.setBuild(null))
                    .subscribe();
            });
    }

    //@CacheEvict(allEntries = true, value = "getServerInfo")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerInfoCache() {}
}
