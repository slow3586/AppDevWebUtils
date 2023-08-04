package ru.blogic.muzedodevwebutils.info;

public record GetServerLogRequest(
    int serverId,
    int logLast
) {}
