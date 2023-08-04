package ru.blogic.muzedodevwebutils.command;

public record CommandRunRequest(
    int serverId,
    String commandId,
    String comment,
    int delaySeconds
){}
