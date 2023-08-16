package ru.blogic.muzedodevwebutils.api.command.dto;

public record CommandDelayRequest(
    int serverId,
    int delaySeconds,
    String comment
) {}
