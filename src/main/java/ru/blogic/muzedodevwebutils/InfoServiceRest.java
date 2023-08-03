package ru.blogic.muzedodevwebutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @GetMapping(path = "overview/{last}", produces = "application/json")
    public Map<Integer, List<InfoEntry>> overviewGet(
        @PathVariable("last") int last
    ) {
        return infoService.getOverview(last);
    }

    @GetMapping(path = "server/{serverId}/{last}", produces = "application/json")
    public List<InfoEntry> overviewGet(
        @PathVariable("serverId") int serverId,
        @PathVariable("last") int last
    ) {
        return infoService.getServerInfo(serverId, last);
    }
}
