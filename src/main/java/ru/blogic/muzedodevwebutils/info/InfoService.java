package ru.blogic.muzedodevwebutils.info;

import io.vavr.control.Option;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import ru.blogic.muzedodevwebutils.command.Command;
import ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect;
import ru.blogic.muzedodevwebutils.server.MuzedoServer;
import ru.blogic.muzedodevwebutils.server.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.server.MuzedoServerService;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.blogic.muzedodevwebutils.server.MuzedoServer.UNKNOWN_BUILD;

@Service
@Slf4j
public class InfoService {
    private final MuzedoServerService muzedoServerService;
    private final MuzedoServerDao muzedoServerDao;
    private static final ThreadLocal<SimpleDateFormat> dateTimeFormat_muzedoBuildInfo = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("HH:mm:ss dd.MM.yyyy z Z"));
    private static final ThreadLocal<SimpleDateFormat> dateTimeFormat_appBuildInfo = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("dd.MM.yy_HH.mm"));
    private WebClient client = WebClient.create();
    private static final String path0 = "UZDO/api/app/buildInfo";
    private static final String path1 = "UZDO-ui/rest/app/buildInfo";

    public InfoService(
        MuzedoServerService muzedoServerService,
        MuzedoServerDao muzedoServerDao
    ) {
        this.muzedoServerService = muzedoServerService;
        this.muzedoServerDao = muzedoServerDao;
    }

    @PostConstruct
    public void postConstruct() {
        try {
            Hooks.onErrorDropped(error -> {
                if (!StringUtils.containsIgnoreCase(error.getMessage(), "503 Service Unavailable")) {
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
        final var muzedoServer = muzedoServerDao.get(serverId);

        return new GetServerInfoResponse(
            muzedoServer.getSshClientSession() != null
                && muzedoServer.getSshClientSession().isOpen()
                && muzedoServer.getWsadminShell() != null
                && muzedoServer.getWsadminShell().isOpen()
                && !Option.of(muzedoServer.getExecutingCommand())
                .map(Command::blocks)
                .contains(Command.Block.SERVER),
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
            Option.of(muzedoServer.getGpBuildInfo())
                .map(MuzedoServer.MuzedoBuildInfo::shortInfo)
                .getOrNull(),
            Option.of(muzedoServer.getIntegBuildInfo())
                .map(MuzedoServer.MuzedoBuildInfo::shortInfo)
                .getOrNull()
        );
    }

    private MuzedoServer.MuzedoBuildInfo parseBuildInfoLines(
        final String buildInfo
    ) {
        final var lines = Arrays.stream(
                StringUtils.split(
                    StringUtils.defaultString(buildInfo),
                    "\n"))
            .map(l -> StringUtils
                .substringAfter(l, ":").trim())
            .toList();
        if (lines.size() < 3) {
            return new MuzedoServer.MuzedoBuildInfo(null,
                null,
                null,
                null,
                UNKNOWN_BUILD);
        }
        return new MuzedoServer.MuzedoBuildInfo(lines.get(1),
            lines.get(2),
            Try.of(() ->
                    dateTimeFormat_muzedoBuildInfo.get()
                        .parse(lines.get(2)))
                .getOrNull(),
            lines.get(3),
            lines.get(1)
                + " @ "
                + StringUtils.substring(lines.get(2),
                    0,
                    22)
                .trim());
    }

    private void doOnErr(
        final MuzedoServer server,
        final Throwable err
    ) {
        server.setGpBuildInfo(null);
        if (!StringUtils.startsWith(err.getMessage(), "503 Service Unavailable")) {
            log.error("#updateInfo {} {}",
                server.getId(),
                err.getMessage());
        }
    }

    private String buildInfoToBuild(
        final MuzedoServer.MuzedoBuildInfo buildInfo
    ) {
        return StringUtils.substringAfterLast(
            buildInfo.branch(), "/")
            + "_" +
            dateTimeFormat_appBuildInfo.get().format(buildInfo.date());
    }

    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void updateInfo() {
        muzedoServerDao
            .getAll()
            .parallelStream()
            .forEach(server -> {
                final var getGP = client.get()
                    .uri(server.getUri() + "/" + path0)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(answer -> server.setGpBuildInfo(parseBuildInfoLines(answer)))
                    .doOnError(e -> this.doOnErr(server, e))
                    .then();

                final var getInteg = client.get()
                    .uri(server.getUri() + "/" + path1)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(answer -> server.setIntegBuildInfo(parseBuildInfoLines(answer)))
                    .doOnError(e -> this.doOnErr(server, e))
                    .then();

                getGP.and(getInteg)
                    .then()
                    .doOnSuccess(a -> {
                        final var gpBuildInfo = server.getGpBuildInfo();
                        final var integBuildInfo = server.getIntegBuildInfo();
                        if (gpBuildInfo != null && gpBuildInfo.date() != null
                            && integBuildInfo != null && integBuildInfo.date() != null) {
                            server.setBuild(buildInfoToBuild(
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

    @CacheEvict(allEntries = true, value = "getServerLog")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerLogCache() {}

    public void writeInfo(
        final int serverId,
        final MuzedoServer.LogEntry.Severity severity,
        final String text
    ) {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var user = auth == null
            ? "Система"
            : ((User) auth.getPrincipal()).getUsername();

        final var infoEntry = new MuzedoServer.LogEntry(
            new Date(),
            text,
            severity,
            user
        );

        muzedoServerDao
            .get(serverId)
            .getLogs()
            .add(infoEntry);
        clearGetServerInfoCache();
        clearGetServerLogCache();
    }

    @DisableLoggingAspect
    @Cacheable(value = "getServerLog")
    public GetServerLogResponse getServerLog(
        final int serverId,
        final int last
    ) {
        final var log = muzedoServerDao
            .get(serverId)
            .getLogs();
        final var infoEntries = log
            .stream()
            .skip(Math.min(log.size(),
                Math.max(log.size() - last < 100 ? last : log.size() - 100, 0)))
            .toList();

        return new GetServerLogResponse(
            infoEntries,
            log.size()
        );
    }
}
