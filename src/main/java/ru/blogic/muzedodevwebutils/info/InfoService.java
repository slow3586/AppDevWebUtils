package ru.blogic.muzedodevwebutils.info;

import io.vavr.control.Option;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.command.Command;
import ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect;
import ru.blogic.muzedodevwebutils.server.MuzedoServer;
import ru.blogic.muzedodevwebutils.server.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.server.MuzedoServerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class InfoService {
    private final MuzedoServerService muzedoServerService;
    private final MuzedoServerDao muzedoServerDao;
    private static final ThreadLocal<SimpleDateFormat> dateTimeFormat_ddMM_HHmmss = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("dd.MM HH:mm:ss"));

    @Autowired
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DisableLoggingAspect
    @Cacheable(value = "getServerInfo")
    public GetServerInfoResponse getServerInfo(int serverId) {
        final var muzedoServer = muzedoServerDao.get(serverId);

        return new GetServerInfoResponse(
            muzedoServer.getChannelShell().isOpen(),
            Option.of(muzedoServer
                    .getChannelShellCurrentCommand()
                    .get())
                .map(Command::name)
                .getOrElse("")
                +
                Option.of(muzedoServer.getScheduledCommandFuture())
                    .map(c -> " через " + c.getDelay(TimeUnit.SECONDS) + " сек. ")
                    .getOrElse("")
        );
    }

    @CacheEvict(allEntries = true, value = "getServerInfo")
    @Scheduled(fixedDelay = 1000)
    @DisableLoggingAspect
    public void clearGetServerInfoCache() {}

    @CacheEvict(allEntries = true, value = "getServerLog")
    @Scheduled(fixedDelay = 1000)
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
            .getLog()
            .add(infoEntry);
    }

    @DisableLoggingAspect
    @Cacheable(value = "getServerLog")
    public GetServerLogResponse getServerLog(
        final int serverId,
        final int last
    ) {
        final var log = muzedoServerDao
            .get(serverId)
            .getLog();
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
