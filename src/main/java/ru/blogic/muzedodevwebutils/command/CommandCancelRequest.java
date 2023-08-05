package ru.blogic.muzedodevwebutils.command;

public record CommandCancelRequest(
    int serverId,
    String comment,
    boolean silent
) {}
