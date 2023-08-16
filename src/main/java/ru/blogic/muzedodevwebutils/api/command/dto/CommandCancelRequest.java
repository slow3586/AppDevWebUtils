package ru.blogic.muzedodevwebutils.api.command.dto;

public record CommandCancelRequest(
    int serverId,
    String comment,
    boolean silent
) {}
