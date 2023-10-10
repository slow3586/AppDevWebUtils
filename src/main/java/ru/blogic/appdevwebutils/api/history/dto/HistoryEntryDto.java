package ru.blogic.appdevwebutils.api.history.dto;

import lombok.Value;
import ru.blogic.appdevwebutils.api.history.repo.HistoryEntry;

import java.util.Date;

@Value
public class HistoryEntryDto {
    int serverId;
    String text;
    HistoryEntry.Severity severity;
    String user;
    Date date;
}
