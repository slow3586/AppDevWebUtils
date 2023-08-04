package ru.blogic.muzedodevwebutils.server;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import ru.blogic.muzedodevwebutils.command.Command;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Data
@RequiredArgsConstructor
public class MuzedoServer {
    final int id;
    final String host;
    ClientSession clientSession;
    ChannelShell channelShell;
    AtomicReference<Command> channelShellCurrentCommand = new AtomicReference<>(null);
    Optional<Command> currentCommand = Optional.empty();
    boolean isGPRunning = false;
    boolean isIntegRunning = false;
    boolean isServerOn = false;
    List<LogEntry> log = new ArrayList<>();

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
