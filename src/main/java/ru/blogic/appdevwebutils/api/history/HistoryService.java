package ru.blogic.appdevwebutils.api.history;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.history.dto.GetServerHistoryResponse;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntry;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntryRepository;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

/**
 * Сервис, отвечающий за предоставление истории операций, запущенных на серверах приложений,
 * а так же создание новых записей.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HistoryService {
    HistoryEntryRepository historyEntryRepository;

    /**
     * Создает запись в истории операций для указанного сервера.
     */
    public void addHistoryEntry(
        final int serverId,
        final HistoryEntry.Severity severity,
        final String text
    ) {
        historyEntryRepository.save(
            HistoryEntry.builder()
                .serverId(serverId)
                .severity(severity)
                .text(text)
                .username(Option.of(SecurityContextHolder.getContext().getAuthentication())
                    .map(auth -> ((User) auth.getPrincipal()).getUsername())
                    .getOrElse("Система"))
                .build());

        this.clearGetServerLogCache();
    }

    /**
     * Предоставляет истории операций указанного сервера, начиная с указанной по ID операции.
     */
    @DisableLoggingAspect
    //@Cacheable(value = "getServerLog", key = "serverId")
    public GetServerHistoryResponse getServerHistory(
        final int serverId,
        final int last
    ) {
        return new GetServerHistoryResponse(
            historyEntryRepository.findByServerIdOrderByDate(serverId)
                .drop(last)
                .map(entry -> new GetServerHistoryResponse.HistoryEntryDto(
                    entry.getServerId(),
                    entry.getText(),
                    entry.getSeverity(),
                    entry.getUsername(),
                    entry.getDate())),
            historyEntryRepository.countByServerId(serverId));
    }

    /**
     * Отвечает за очистку кэша историй операций серверов.
     */
    //@CacheEvict(allEntries = true, value = "getServerLog")
    @DisableLoggingAspect
    public void clearGetServerLogCache() {}
}
