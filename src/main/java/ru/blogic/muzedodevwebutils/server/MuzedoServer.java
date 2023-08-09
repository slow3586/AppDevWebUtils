package ru.blogic.muzedodevwebutils.server;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import ru.blogic.muzedodevwebutils.command.Command;
import ru.blogic.muzedodevwebutils.info.GetServerInfoResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class MuzedoServer {
    final int id;
    final String host;
    final String uri;
    final String p;

    String build;
    MuzedoBuildInfo gpBuildInfo;
    MuzedoBuildInfo integBuildInfo;

    ClientSession sshClientSession;
    ChannelShell wsadminShell;

    List<LogEntry> logs = new ArrayList<>();

    final ReentrantLock commandSchedulingLock = new ReentrantLock();
    final ReentrantLock wsadminConnectLock = new ReentrantLock();
    final ReentrantLock sessionConnectLock = new ReentrantLock();

    ScheduledCommand scheduledCommand = null;

    Command executingCommand = null;
    final AtomicInteger executingCommandTimer = new AtomicInteger(0);

    public static String UNKNOWN_BUILD = "Неизвестная сборка";

    public record LogEntry(
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
        ScheduledFuture<String> future,
        Callable<String> callable
    ) {}
}
