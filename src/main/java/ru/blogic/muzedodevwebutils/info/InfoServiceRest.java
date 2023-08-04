package ru.blogic.muzedodevwebutils.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpHeaders;

@RestController
@RequestMapping("api/info")
public class InfoServiceRest {
    private final InfoService infoService;

    @Autowired
    public InfoServiceRest(
        InfoService infoService
    ) {
        this.infoService = infoService;
    }

    @GetMapping(path = "getServerInfo/{serverId}", produces = "application/json")
    public GetServerInfoResponse getServerInfo(
        @PathVariable final int serverId
    ) {
        return infoService.getServerInfo(serverId);
    }

    @GetMapping(path = "getServerLog/{serverId}", produces = "application/json")
    public GetServerLogResponse getServerLog(
        @PathVariable final int serverId,
        @RequestParam final int last
    ) {
        return infoService.getServerLog(serverId, last);
    }
}
