package ru.blogic.appdevwebutils.api.history;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.history.dto.GetServerHistoryResponse;
import ru.blogic.appdevwebutils.api.history.dto.HistoryEntryDto;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntry;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntryRepository;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HistoryService {
    HistoryEntryRepository historyEntryRepository;

    public void addHistoryEntry(
        final int serverId,
        final HistoryEntry.Severity severity,
        final String text
    ) {
        final HistoryEntry historyEntry = new HistoryEntry();
        historyEntry.setServerId(serverId);
        historyEntry.setSeverity(severity);
        historyEntry.setText(text);
        historyEntry.setUsername(
            Option.of(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> ((User) auth.getPrincipal()).getUsername())
                .getOrElse("Система"));
        historyEntryRepository.save(historyEntry);

        this.clearGetServerLogCache();
    }

    @DisableLoggingAspect
    //@Cacheable(value = "getServerLog", key = "serverId")
    public GetServerHistoryResponse getServerHistory(
        final int serverId,
        final int last
    ) {
        return new GetServerHistoryResponse(
            historyEntryRepository.findByServerIdOrderByDate(serverId)
                .drop(last)
                .map(entry -> new HistoryEntryDto(
                    entry.getServerId(),
                    entry.getText(),
                    entry.getSeverity(),
                    entry.getUsername(),
                    entry.getDate())),
            historyEntryRepository.countByServerId(serverId));
    }

    //@CacheEvict(allEntries = true, value = "getServerLog")
    @DisableLoggingAspect
    public void clearGetServerLogCache() {}
}
