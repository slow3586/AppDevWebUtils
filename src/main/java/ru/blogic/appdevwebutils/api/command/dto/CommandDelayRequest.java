package ru.blogic.appdevwebutils.api.command.dto;

public record CommandDelayRequest(
    int serverId,
    int delaySeconds,
    String comment
) {}
