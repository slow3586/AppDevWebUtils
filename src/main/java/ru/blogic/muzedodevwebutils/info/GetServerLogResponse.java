package ru.blogic.muzedodevwebutils.info;

import ru.blogic.muzedodevwebutils.server.MuzedoServer;

import java.util.List;

public record GetServerLogResponse(
    List<MuzedoServer.LogEntry> logs,
    int logLast
) {}
