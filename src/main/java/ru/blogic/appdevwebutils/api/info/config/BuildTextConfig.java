package ru.blogic.appdevwebutils.api.info.config;

import java.time.format.DateTimeFormatter;

public record BuildTextConfig(
    String textFormat,
    int hashLength,
    DateTimeFormatter dateTimeFormat
) {}
