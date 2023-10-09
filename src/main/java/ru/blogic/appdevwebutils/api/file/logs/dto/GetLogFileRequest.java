package ru.blogic.appdevwebutils.api.file.logs.dto;

public record GetLogFileRequest(
    int serverId,
    String logId,
    int linesCount
) {}
