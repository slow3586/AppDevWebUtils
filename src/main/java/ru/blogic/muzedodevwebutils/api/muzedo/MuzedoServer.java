package ru.blogic.muzedodevwebutils.api.muzedo;

import io.vavr.collection.Vector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.utils.Timer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class MuzedoServer {
    final int id;
    final String host;
    final String password;
    final FilePaths filePaths;

    String build;
    MuzedoBuildInfo gpBuildInfo;
    MuzedoBuildInfo integBuildInfo;

    ClientSession sshClientSession;
    ChannelShell wsadminShell;

    final ConcurrentLinkedQueue<HistoryEntry> history = new ConcurrentLinkedQueue<>();

    final ReentrantLock commandSchedulingLock = new ReentrantLock();
    final ReentrantLock wsadminConnectLock = new ReentrantLock();
    final ReentrantLock sessionConnectLock = new ReentrantLock();

    ScheduledCommand scheduledCommand = null;

    Command executingCommand = null;
    Timer executingCommandTimer = new Timer();
    Timer delayBetweenCommands = new Timer();

    public static String UNKNOWN_BUILD = "Неизвестная сборка";

    public record HistoryEntry(
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

    public record MuzedoBuildInfo(
        String author,
        String date,
        String branch,
        String hash
    ) {}

    public record ScheduledCommand(
        Command command,
        ScheduledFuture<?> future,
        Runnable callable
    ) {}

    public record FilePaths(
        String configsFilePath,
        String logsFilePath
    ){}
}
