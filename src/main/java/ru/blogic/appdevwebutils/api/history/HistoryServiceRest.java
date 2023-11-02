package ru.blogic.appdevwebutils.api.history;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.appdevwebutils.api.history.dto.GetServerHistoryResponse;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntry;

/**
 * REST сервис, предоставляющий историю операций, запущенных в веб приложении пользователями.
 */
@RestController
@RequestMapping("api/history")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class HistoryServiceRest {
    HistoryService historyService;

    /**
     * Предоставляет историю операций, проведенных на указано сервере приложения.
     */
    @GetMapping(path = "{serverId}", produces = "application/json")
    public GetServerHistoryResponse getServerLog(
        @PathVariable final int serverId,
        @RequestParam final int last
    ) {
        return historyService.getServerHistory(serverId, last);
    }

    /**
     * Предоставляет возможность добавить запись в историю операций другим сервисам.
     */
    @PostMapping(path = "{serverId}", produces = "application/json")
    public void addHistoryEntry(
        final int serverId,
        final HistoryEntry.Severity severity,
        final String text
    ) {
        historyService.addHistoryEntry(serverId, severity, text);
    }
}
