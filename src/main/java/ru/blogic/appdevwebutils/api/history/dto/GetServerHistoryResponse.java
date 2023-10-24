package ru.blogic.appdevwebutils.api.history.dto;

import io.vavr.collection.List;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntry;

import java.util.Date;

/**
 * Хранит информацию об ответе на запрос истории операций.
 * @param logs Список записей в истории операций.
 * @param logLast ID последней записи.
 */
public record GetServerHistoryResponse(
    List<HistoryEntryDto> logs,
    int logLast
) {
    /**
     * Хранит информацию о записи в истории операций.
     * @param serverId ID сервера.
     * @param text Текст записи.
     * @param severity Важность записи.
     * @param user Пользователь, связанный с записью.
     * @param date Дата и время записи.
     */
    public record HistoryEntryDto(
        int serverId,
        String text,
        HistoryEntry.Severity severity,
        String user,
        Date date
    ) {}
}
