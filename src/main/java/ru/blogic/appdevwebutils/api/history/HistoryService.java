package ru.blogic.appdevwebutils.api.history;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.history.dto.GetServerHistoryResponse;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;
import ru.blogic.appdevwebutils.utils.Utils;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HistoryService {
    AppServerConfig appServerConfig;

    public void addHistoryEntry(
        final int serverId,
        final AppServer.HistoryEntry.Severity severity,
        final String text
    ) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final String user = auth == null
            ? "Система"
            : ((User) auth.getPrincipal()).getUsername();

        final AppServer.HistoryEntry infoEntry = new AppServer.HistoryEntry(
            new Date(),
            text,
            severity,
            user);

        appServerConfig
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
        final ConcurrentLinkedQueue<AppServer.HistoryEntry> log = appServerConfig
            .get(serverId)
            .getHistory();
        final List<AppServer.HistoryEntry> infoEntries =
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

    @CacheEvict(allEntries = true, value = "getServerLog")
    @DisableLoggingAspect
    public void clearGetServerLogCache() {}
}
