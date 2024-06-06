package ru.blogic.appdevwebutils.api.app;

import io.vavr.collection.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import ru.blogic.appdevwebutils.api.app.ssh.SshConnection;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.utils.TimerScheduler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Сущность для хранения актуальной информации о сервере приложения.
 */
@Data
@RequiredArgsConstructor
public class AppServer {
    /**
     * ID сервера.
     */
    final int id;
    /**
     * Адрес сервера.
     */
    final String host;
    /**
     * Пути к файлам SSH сервера.
     */
    final FilePaths filePaths;

    /**
     * Информация о сборке приложения.
     */
    String appBuildText;
    /**
     * Информация о сборке модулей приложения.
     */
    List<ModuleBuildInfo> moduleBuildInfoList;

    /**
     * SSH сессия.
     */
    ClientSession sshClientSession;
    /**
     * SSH сессия для WsAdmin.
     */
    ChannelShell wsadminShell;
    /**
     * SSH SCP сессия.
     */
    ScpClient scpClient;

    /**
     * Lock выполнения операции.
     */
    final ReentrantLock commandSchedulingLock = new ReentrantLock();
    /**
     * Lock выполнения операции в wsAdmin.
     */
    final ReentrantLock wsadminConnectLock = new ReentrantLock();
    /**
     * Lock создания сессии SSH.
     */
    final ReentrantLock sessionConnectLock = new ReentrantLock();

    /**
     * Запланированная операция.
     */
    ScheduledCommand scheduledCommand = null;
    /**
     * Выполняемая в данный момент операция.
     */
    Command executingCommand = null;
    /**
     * Таймер выполняемой операции.
     */
    TimerScheduler.Timer executingCommandTimer = new TimerScheduler.Timer();

    /**
     * Пул SSH соединений.
     */
    final java.util.List<SshConnection> sshConnectionPool = new ArrayList<>();
    /**
     * Lock для изменений пула SSH соединений.
     */
    final ReentrantLock sshConnectionPoolLock = new ReentrantLock();

    /**
     * Сущность для хранения информации о сборке модуля приложения.
     *
     * @param name         Название модуля.
     * @param hasBuildInfo Получена ли внешняя информация о сборке модуля.
     * @param online       Онлайн ли модуль.
     * @param author       Автор сборки модуля.
     * @param date         Дата сборки модуля.
     * @param branch       Ветка сборки модуля.
     * @param hash         Хэш сборки модуля.
     */
    public record ModuleBuildInfo(
        String name,
        boolean hasBuildInfo,
        boolean online,
        String author,
        ZonedDateTime date,
        String branch,
        String hash
    ) {}

    /**
     * Сущность для хранения информации о запланированной операции.
     *
     * @param command         Информация об операции.
     * @param scheduledFuture Future выполнения операции.
     * @param runnable        Runnable выполнения операции.
     */
    public record ScheduledCommand(
        Command command,
        ScheduledFuture<?> scheduledFuture,
        Runnable runnable
    ) {}

    /**
     * Сущность для хранения путей к файлам на FTP приложения.
     *
     * @param configs Путь к папке с конфигами.
     * @param logs    Путь к папке с логами.
     */
    public record FilePaths(
        String configs,
        String logs
    ) {}
}
