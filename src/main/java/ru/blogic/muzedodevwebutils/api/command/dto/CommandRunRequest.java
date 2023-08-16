package ru.blogic.muzedodevwebutils.api.command.dto;

public record CommandRunRequest(
    int serverId,
    String commandId,
    String comment,
    int delaySeconds
){}
