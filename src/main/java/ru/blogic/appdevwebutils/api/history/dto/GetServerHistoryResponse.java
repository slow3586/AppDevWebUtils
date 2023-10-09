package ru.blogic.appdevwebutils.api.history.dto;

import ru.blogic.appdevwebutils.api.app.AppServer;

public record GetServerHistoryResponse(
    io.vavr.collection.List<AppServer.HistoryEntry> logs,
    int logLast
) {}
