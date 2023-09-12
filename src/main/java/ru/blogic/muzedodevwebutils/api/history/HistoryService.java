package ru.blogic.muzedodevwebutils.api.history;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.history.dto.GetServerHistoryResponse;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.config.MuzedoServerConfig;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;
import ru.blogic.muzedodevwebutils.utils.Utils;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HistoryService {
    MuzedoServerConfig muzedoServerConfig;

    @CacheEvict(allEntries = true, value = "getServerLog")
    @DisableLoggingAspect
    public void clearGetServerLogCache() {}

    public void addHistoryEntry(
        final int serverId,
        final MuzedoServer.HistoryEntry.Severity severity,
        final String text
    ) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final String user = auth == null
            ? "Система"
            : ((User) auth.getPrincipal()).getUsername();

        final MuzedoServer.HistoryEntry infoEntry = new MuzedoServer.HistoryEntry(
            new Date(),
            text,
            severity,
            user);

        muzedoServerConfig
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
        final ConcurrentLinkedQueue<MuzedoServer.HistoryEntry> log = muzedoServerConfig
            .get(serverId)
            .getHistory();
        final List<MuzedoServer.HistoryEntry> infoEntries =
            List.ofAll(log)
                .drop(Utils.clamp(
                    log.size() - last < 100 ? last : log.size() - 100,
                    0,
                    log.size()));

        return new GetServerHistoryResponse(
            infoEntries,
            log.size()
        );
    }
}
