package ru.blogic.appdevwebutils.api.app;

import io.vavr.collection.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.api.app.ssh.SshConnection;
import ru.blogic.appdevwebutils.utils.Timer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class AppServer {
    final int id;
    final String host;
    final String password;
    final FilePaths filePaths;

    String appBuildText;
    List<ModuleBuildInfo> moduleBuildInfoList;

    ClientSession sshClientSession;
    ChannelShell wsadminShell;
    ScpClient scpClient;

    final ConcurrentLinkedQueue<HistoryEntry> history = new ConcurrentLinkedQueue<>();

    final ReentrantLock commandSchedulingLock = new ReentrantLock();
    final ReentrantLock wsadminConnectLock = new ReentrantLock();
    final ReentrantLock sessionConnectLock = new ReentrantLock();

    ScheduledCommand scheduledCommand = null;

    Command executingCommand = null;
    Timer executingCommandTimer = new Timer();
    Timer delayBetweenCommands = new Timer();

    final ReentrantLock SSHConnectionPoolLock = new ReentrantLock();
    final java.util.List<SshConnection> SshConnectionPool = new ArrayList<>();

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

    public record ModuleBuildInfo(
        String name,
        boolean online,
        String author,
        Instant date,
        String branch,
        String hash
    ) {}

    public record ScheduledCommand(
        Command command,
        ScheduledFuture<?> scheduledFuture,
        Runnable runnable
    ) {}

    public record FilePaths(
        String configsFilePath,
        String logsFilePath
    ){}
}
