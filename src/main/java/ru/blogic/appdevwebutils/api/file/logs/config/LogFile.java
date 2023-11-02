package ru.blogic.appdevwebutils.api.file.logs.config;

/**
 * Информация о файле лога
 *
 * @param id   ID файла
 * @param path Путь к файлу на FTP
 */
public record LogFile(
    String id,
    String path
) {}
