package ru.blogic.appdevwebutils.api.info;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.appdevwebutils.api.info.dto.GetServerInfoResponse;

/**
 * REST сервис, отвечающий за предоставление информации о сборке приложения на стенде.
 */
@RestController
@RequestMapping("api/info")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoServiceRest {
    InfoService infoService;

    /**
     * Предоставляет информацию о сборке приложения.
     */
    @GetMapping(path = "{serverId}", produces = "application/json")
    public GetServerInfoResponse getServerInfo(
        @PathVariable final int serverId
    ) {
        return infoService.getServerInfo(serverId);
    }
}
