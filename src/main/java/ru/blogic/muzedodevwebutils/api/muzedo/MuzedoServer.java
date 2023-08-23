package ru.blogic.muzedodevwebutils.api.muzedo;

import io.vavr.collection.Vector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import ru.blogic.muzedodevwebutils.api.command.Command;

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
    final String password;

    String build;
    MuzedoBuildInfo gpBuildInfo;
    MuzedoBuildInfo integBuildInfo;

    ClientSession sshClientSession;
    ChannelShell wsadminShell;

    final Vector<HistoryEntry> history = Vector.empty();

    final ReentrantLock commandSchedulingLock = new ReentrantLock();
    final ReentrantLock wsadminConnectLock = new ReentrantLock();
    final ReentrantLock sessionConnectLock = new ReentrantLock();

    ScheduledCommand scheduledCommand = null;

    Command executingCommand = null;
    final AtomicInteger executingCommandTimer = new AtomicInteger(0);

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
        ScheduledFuture<String> future,
        Callable<String> callable
    ) {}
}
