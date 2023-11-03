package ru.blogic.appdevwebutils.api.info.dto;

import io.vavr.collection.List;
import ru.blogic.appdevwebutils.api.command.Command;

/**
 * DTO информации о текущих сборке и операциях на одном сервере приложения.
 *
 * @param wsAdminShell          Запущена ли оболочка WsAdmin
 * @param scheduledCommand      Запланированная операция
 * @param executingCommand      Текущая операция
 * @param executingCommandTimer Время выполнения текущей операции
 * @param scheduledCommandTimer Таймер запуска запланированной операции
 * @param appBuildText          Информация о сборке приложения
 * @param moduleBuildInfoList   Информация о сборках модулей приложения
 */
public record GetServerInfoResponse(
    boolean wsAdminShell,
    Command scheduledCommand,
    Command executingCommand,
    int executingCommandTimer,
    int scheduledCommandTimer,
    String appBuildText,
    List<ModuleBuildInfo> moduleBuildInfoList
) {
    /**
     * Информация о сборке модуля приложения
     */
    public record ModuleBuildInfo(
        String name,
        String buildText
    ) {}
}
