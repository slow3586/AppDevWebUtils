package ru.blogic.appdevwebutils.api.history;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.appdevwebutils.api.history.dto.GetServerHistoryResponse;

/**
 * REST сервис, предоставляющий историю операций, запущенных в веб приложении пользователями.
 */
@RestController
@RequestMapping("api/history")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
}
