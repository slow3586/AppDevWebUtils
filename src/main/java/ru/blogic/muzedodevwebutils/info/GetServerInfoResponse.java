package ru.blogic.muzedodevwebutils.info;

import ru.blogic.muzedodevwebutils.command.Command;
import ru.blogic.muzedodevwebutils.server.MuzedoServer;

public record GetServerInfoResponse(
    boolean wsAdminShell,
    Command scheduledCommand,
    Command executingCommand,
    int executingCommandTimer,
    int scheduledCommandTimer,
    String build,
    MuzedoServer.MuzedoBuildInfo gpBuild,
    MuzedoServer.MuzedoBuildInfo integBuild
) {
}
