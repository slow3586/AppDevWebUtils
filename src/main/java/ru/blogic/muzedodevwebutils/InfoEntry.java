package ru.blogic.muzedodevwebutils;

import java.util.Date;

public record InfoEntry (
    Date date,
    String text,
    Severity severity,
    String user
) {
    public enum Severity {
        CRIT,
        INFO,
        TRACE
    }
}
