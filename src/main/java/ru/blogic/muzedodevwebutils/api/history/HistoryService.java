package ru.blogic.muzedodevwebutils.api.history;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HistoryService {
    MuzedoServerDao muzedoServerDao;

    @CacheEvict(allEntries = true, value = "getServerLog")
    @Scheduled(fixedDelay = 3000)
    @DisableLoggingAspect
    public void clearGetServerLogCache() {}

    public void writeInfo(
        final int serverId,
        final MuzedoServer.HistoryEntry.Severity severity,
        final String text
    ) {
        val auth = SecurityContextHolder.getContext().getAuthentication();
        val user = auth == null
            ? "Система"
            : ((User) auth.getPrincipal()).getUsername();

        val infoEntry = new MuzedoServer.HistoryEntry(
            new Date(),
            text,
            severity,
            user
        );

        muzedoServerDao
            .get(serverId)
            .getHistory()
            .add(infoEntry);

        this.clearGetServerLogCache();
    }

    @DisableLoggingAspect
    @Cacheable(value = "getServerLog")
    public GetServerHistoryResponse getServerHistory(
        final int serverId,
        final int last
    ) {
        val log = muzedoServerDao
            .get(serverId)
            .getHistory();
        val infoEntries = log
            .stream()
            .skip(Math.min(log.size(),
                Math.max(log.size() - last < 100 ? last : log.size() - 100, 0)))
            .toList();

        return new GetServerHistoryResponse(
            infoEntries,
            log.size()
        );
    }

    public record GetServerHistoryResponse(
        List<MuzedoServer.HistoryEntry> logs,
        int logLast
    ) {}
}
