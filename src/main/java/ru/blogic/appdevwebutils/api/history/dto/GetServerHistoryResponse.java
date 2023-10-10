package ru.blogic.appdevwebutils.api.history.dto;

import io.vavr.collection.List;

public record GetServerHistoryResponse(
    List<HistoryEntryDto> logs,
    int logLast
) {}
