package ru.blogic.muzedodevwebutils.server;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import ru.blogic.muzedodevwebutils.command.Command;

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

    String gpStatus;
    String integStatus;

    ClientSession sshClientSession;
    ChannelShell wsadminShell;

    List<LogEntry> logs = new ArrayList<>();

    final ReentrantLock commandSchedulingLock = new ReentrantLock();
    final ReentrantLock wsadminConnectLock = new ReentrantLock();
    final ReentrantLock sessionConnectLock = new ReentrantLock();

    ScheduledCommand scheduledCommand = null;

    Command executingCommand = null;
    final AtomicInteger executingCommandTimer = new AtomicInteger(0);

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

    public record ScheduledCommand(
        Command command,
        ScheduledFuture<String> future,
        Callable<String> callable
    ) {}
}
