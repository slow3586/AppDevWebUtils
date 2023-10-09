package ru.blogic.appdevwebutils.api.info.dto;

import io.vavr.collection.List;
import ru.blogic.appdevwebutils.api.command.Command;

public record GetServerInfoResponse(
    boolean wsAdminShell,
    Command scheduledCommand,
    Command executingCommand,
    int executingCommandTimer,
    int scheduledCommandTimer,
    String appBuildText,
    List<ModuleBuildInfo> moduleBuildInfoList
) {
    public record ModuleBuildInfo (
        String name,
        String buildText
    ) {}
}
