package ru.blogic.appdevwebutils.api.front.dto;

import io.vavr.collection.List;

public record GetFrontendConfigResponse(
    String version,
    List<GetFrontendConfigResponseCommand> commands,
    List<Integer> servers,
    List<GetFrontendConfigResponseConfig> configs,
    List<GetFrontendConfigResponseLog> logs
) {
    public record GetFrontendConfigResponseCommand(
        String id,
        String name,
        boolean blocksWsadmin
    ) {}

    public record GetFrontendConfigResponseConfig(
        String id
    ) {}

    public record GetFrontendConfigResponseLog(
        String id
    ) {}
}
