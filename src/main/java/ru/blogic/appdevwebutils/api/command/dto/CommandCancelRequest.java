package ru.blogic.appdevwebutils.api.command.dto;

public record CommandCancelRequest(
    int serverId,
    String comment,
    boolean silent
) {}
