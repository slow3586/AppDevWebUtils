package ru.blogic.appdevwebutils.api.file.logs.dto;

/**
 * Информация о запросе файла лога сервера.
 *
 * @param serverId   ID сервера
 * @param logId      ID лога
 * @param linesCount Кол-во строк с конца
 */
public record GetLogFileRequest(
    int serverId,
    String logId,
    int linesCount
) {}
