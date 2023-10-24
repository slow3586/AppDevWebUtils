package ru.blogic.appdevwebutils.api.front.dto;

import io.vavr.collection.List;

/**
 * Хранит конфигурацию frontend-клиента.
 * @param version Версия приложения.
 * @param commands Список доступных операций над серверами приложений.
 * @param servers Сервера приложений
 * @param configs Список доступных конфиг-файлов серверов приложений.
 * @param logs Список доступных лог-файлов серверов приложений.
 */
public record GetFrontendConfigResponse(
    String version,
    List<GetFrontendConfigResponseCommand> commands,
    List<Integer> servers,
    List<GetFrontendConfigResponseConfig> configs,
    List<GetFrontendConfigResponseLog> logs
) {
    /**
     * Информация об операции.
     * @param id ID операции.
     * @param name Название операции для интерфейса.
     * @param blocksWsadmin Блокирует ли операция WsAdmin.
     */
    public record GetFrontendConfigResponseCommand(
        String id,
        String name,
        boolean blocksWsadmin
    ) {}

    /**
     * Информация о файле конфигурации.
     * @param id ID файла.
     */
    public record GetFrontendConfigResponseConfig(
        String id
    ) {}

    /**
     * Информация о файле лога.
     * @param id ID файла.
     */
    public record GetFrontendConfigResponseLog(
        String id
    ) {}
}
