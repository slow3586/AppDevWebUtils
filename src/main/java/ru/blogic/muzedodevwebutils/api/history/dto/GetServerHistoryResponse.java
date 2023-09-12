package ru.blogic.muzedodevwebutils.api.history.dto;

import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;

public record GetServerHistoryResponse(
    io.vavr.collection.List<MuzedoServer.HistoryEntry> logs,
    int logLast
) {}
