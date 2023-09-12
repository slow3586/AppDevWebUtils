package ru.blogic.muzedodevwebutils.api.file.logs.dto;

public record GetLogFileRequest(
    int serverId,
    String logId,
    int linesCount
) {}
