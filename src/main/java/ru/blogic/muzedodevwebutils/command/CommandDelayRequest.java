package ru.blogic.muzedodevwebutils.command;

public record CommandDelayRequest(
    int serverId,
    int delaySeconds,
    String comment
) {}
