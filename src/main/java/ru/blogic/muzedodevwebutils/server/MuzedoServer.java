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
import java.util.concurrent.atomic.AtomicReference;

@Data
@RequiredArgsConstructor
public class MuzedoServer {
    final int id;
    final String host;
    ClientSession clientSession;
    ChannelShell channelShell;
    AtomicReference<Command> channelShellCurrentCommand = new AtomicReference<>(null);
    boolean isGPRunning = false;
    boolean isIntegRunning = false;
    boolean isServerOn = false;
    List<LogEntry> log = new ArrayList<>();
    //Command scheduledCommand = null;
    ScheduledFuture<String> scheduledCommandFuture = null;
    Callable<String> scheduledCallable = null;
    Command scheduledCommand = null;

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
}
