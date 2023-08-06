package ru.blogic.muzedodevwebutils.info;

import ru.blogic.muzedodevwebutils.command.Command;

public record GetServerInfoResponse(
    boolean wsAdminShell,
    Command scheduledCommand,
    Command executingCommand,
    int executingCommandTimer,
    int scheduledCommandTimer
) {}
