package ru.blogic.muzedodevwebutils.api.info;

import io.vavr.control.Option;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer.UNKNOWN_BUILD;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoService {
    MuzedoServerDao muzedoServerDao;
    static ThreadLocal<SimpleDateFormat> dateTimeFormat_muzedoBuildInfo = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("HH:mm:ss dd.MM.yyyy z Z"));
    static ThreadLocal<SimpleDateFormat> dateTimeFormat_appBuildInfo = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("dd.MM.yy_HH.mm"));
    WebClient client = WebClient.create();
    static String path0 = "UZDO/api/app/buildInfo";
    static String path1 = "UZDO-ui/rest/app/buildInfo";

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
    @Cacheable(value = "getServerInfo")
    public GetServerInfoResponse getServerInfo(int serverId) {
        val muzedoServer = muzedoServerDao.get(serverId);

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
            muzedoServer.getExecutingCommandTimer().get(),
            Option.of(muzedoServer.getScheduledCommand())
                .map(MuzedoServer.ScheduledCommand::future)
                .map(f -> f.getDelay(TimeUnit.SECONDS))
                .map(Math::toIntExact)
                .getOrElse(0),
            muzedoServer.getBuild(),
            muzedoServer.getGpBuildInfo(),
            muzedoServer.getIntegBuildInfo()
        );
    }

    private MuzedoServer.MuzedoBuildInfo parseBuildInfoLines(
        final String buildInfo
    ) {
        val lines = Arrays.stream(
                StringUtils.split(
                    StringUtils.defaultString(buildInfo),
                    "\n"))
            .map(l -> StringUtils
                .substringAfter(l, ":").trim())
            .toList();
        if (lines.size() < 4) {
            return new MuzedoServer.MuzedoBuildInfo(null,
                null,
                null,
                null);
        }
        return new MuzedoServer.MuzedoBuildInfo(lines.get(1),
            lines.get(2),
            lines.get(3),
            StringUtils.substring(lines.get(4), 0, 5));
    }

    private void doOnErr(
        final MuzedoServer server,
        final Throwable err
    ) {
        server.setGpBuildInfo(null);
        server.setIntegBuildInfo(null);
        if (!StringUtils.containsAnyIgnoreCase(err.getMessage(),
            "503 Service Unavailable",
            "Connection refused: no further information")) {
            log.error("#updateInfo {} {}",
                server.getId(),
                err.getMessage());
        }
    }

    private String buildInfoToBuild(
        final String branch,
        final MuzedoServer.MuzedoBuildInfo buildInfo
    ) {
        return Option.of(this.buildVersion)
            .filter(StringUtils::isNotBlank)
            .getOrElse(StringUtils.substringAfterLast(branch, "/"))
            + "_" +
            Try.of(() ->
                dateTimeFormat_appBuildInfo.get().format(
                    dateTimeFormat_muzedoBuildInfo.get().parse(buildInfo.date())
                )
            ).getOrNull();
    }

    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void updateInfo() {
        muzedoServerDao
            .getAll()
            .parallelStream()
            .forEach(server -> {
                val getGP = client.get()
                    .uri(server.getUri() + "/" + path0)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(answer -> server.setGpBuildInfo(parseBuildInfoLines(answer)))
                    .doOnError(e -> this.doOnErr(server, e))
                    .then();

                val getInteg = client.get()
                    .uri(server.getUri() + "/" + path1)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(answer -> server.setIntegBuildInfo(parseBuildInfoLines(answer)))
                    .doOnError(e -> this.doOnErr(server, e))
                    .then();

                getGP.and(getInteg)
                    .then()
                    .doOnSuccess(a -> {
                        val gpBuildInfo = server.getGpBuildInfo();
                        val integBuildInfo = server.getIntegBuildInfo();
                        if (gpBuildInfo != null && gpBuildInfo.date() != null
                            && integBuildInfo != null && integBuildInfo.date() != null) {
                            server.setBuild(buildInfoToBuild(integBuildInfo.branch(),
                                (gpBuildInfo.date().compareTo(integBuildInfo.date()) > 0)
                                    ? gpBuildInfo
                                    : integBuildInfo));
                        } else {
                            server.setBuild(UNKNOWN_BUILD);
                        }
                    })
                    .doOnError(e -> server.setBuild(null))
                    .subscribe();
            });
    }

    @CacheEvict(allEntries = true, value = "getServerInfo")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerInfoCache() {}

    public record GetServerInfoResponse(
        boolean wsAdminShell,
        Command scheduledCommand,
        Command executingCommand,
        int executingCommandTimer,
        int scheduledCommandTimer,
        String build,
        MuzedoServer.MuzedoBuildInfo gpBuild,
        MuzedoServer.MuzedoBuildInfo integBuild
    ) {}
}
