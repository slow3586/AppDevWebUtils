package ru.blogic.muzedodevwebutils.api.info;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/info")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InfoServiceRest {
    InfoService infoService;

    @GetMapping(path = "{serverId}", produces = "application/json")
    public InfoService.GetServerInfoResponse getServerInfo(
        @PathVariable final int serverId
    ) {
        return infoService.getServerInfo(serverId);
    }
}
