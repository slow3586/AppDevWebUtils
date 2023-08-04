package ru.blogic.muzedodevwebutils;

public record RunCommandRequest(
    int serverId,
    String commandId,
    String comment,
    int delaySeconds
){}
