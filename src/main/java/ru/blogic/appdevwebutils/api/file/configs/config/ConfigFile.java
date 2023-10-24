package ru.blogic.appdevwebutils.api.file.configs.config;

/**
 * Информация о файле конфигурации
 * @param id ID файла
 * @param path Путь к файлу в FTP сервера
 */
public record ConfigFile(
    String id,
    String path
) {}
