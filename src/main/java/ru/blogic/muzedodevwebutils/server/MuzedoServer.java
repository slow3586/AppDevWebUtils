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
import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class MuzedoServer {
    final int id;
    final String host;

    ClientSession sshClientSession;
    ChannelShell wsadminShell;

    List<LogEntry> logs = new ArrayList<>();

    final ReentrantLock commandSchedulingLock = new ReentrantLock();

    ScheduledCommand scheduledCommand = null;

    Command executingCommand = null;

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
