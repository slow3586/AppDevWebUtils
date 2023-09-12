package ru.blogic.muzedodevwebutils.api.info.dto;

import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;

public record GetServerInfoResponse(
    boolean wsAdminShell,
    Command scheduledCommand,
    Command executingCommand,
    int executingCommandTimer,
    int scheduledCommandTimer,
    String build,
    MuzedoServer.MuzedoBuildInfo gpBuild,
    MuzedoServer.MuzedoBuildInfo integBuild
) {}
