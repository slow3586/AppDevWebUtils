package ru.blogic.muzedodevwebutils.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.SSHService;
import ru.blogic.muzedodevwebutils.command.CommandDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
public class MuzedoServerService {
    private final CommandDao commandDao;
    private final SSHService sshService;
    private final MuzedoServerDao muzedoServerDao;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

    public MuzedoServerService(
        CommandDao commandDao,
        SSHService sshService,
        MuzedoServerDao muzedoServerDao
    ) {
        this.commandDao = commandDao;
        this.sshService = sshService;
        this.muzedoServerDao = muzedoServerDao;
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60, initialDelay = 0)
    public void scheduleReconnect() {
        try {
            log.debug("#scheduleReconnect");
            muzedoServerDao.getAll()
                .stream()
                .filter(server ->
                    server.getExecutingCommand() == null
                        && server.getScheduledCommand() == null
                        && !server.getCommandSchedulingLock().isLocked()
                        && !server.getWsadminConnectLock().isLocked()
                        && !server.getSessionConnectLock().isLocked())
                .forEach(server -> executorService.submit(
                    () -> reconnectSshSession(server)));
        } catch (Exception e) {
            throw new RuntimeException("#scheduleReconnect " + e.getMessage(), e);
        }
    }

    public void reconnectSshSession(MuzedoServer muzedoServer) {
        if (!muzedoServer.getSessionConnectLock().tryLock()) {
            log.warn("#reconnectSshSession {}: уже в процессе", muzedoServer.host);
            return;
        }
        try {
            if (muzedoServer.getSshClientSession() != null
                && !muzedoServer.getSshClientSession().isClosing()
            ) {
                muzedoServer.getWsadminShell().close();
            }

            final var session = sshService.createSession(muzedoServer.getHost());
            session.addCloseFutureListener((future) -> {
                log.warn(muzedoServer.getHost() + ": закрыт SshSession!");
                reconnectSshSession(muzedoServer);
            });

            muzedoServer.setSshClientSession(session);
            log.debug(muzedoServer.getHost() + ": SshSession установлена!");
        } catch (Exception e) {
            log.error("#reconnectSshSession exception: " + e.getMessage(), e);
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                reconnectSshSession(muzedoServer);
            }).start();
            return;
        } finally {
            muzedoServer.getSessionConnectLock().unlock();
        }

        reconnectWsadminShell(muzedoServer);
    }

    public void reconnectWsadminShell(MuzedoServer muzedoServer) {
        if (muzedoServer.getSessionConnectLock().isLocked()) {
            log.warn("#reconnectWsadminShell {}: reconnectSshSession в процессе", muzedoServer.host);
            return;
        }
        if (!muzedoServer.getWsadminConnectLock().tryLock()) {
            log.warn("#reconnectWsadminShell {}: уже в процессе", muzedoServer.host);
            return;
        }
        try {
            if (muzedoServer.getSshClientSession() == null
                || muzedoServer.getSshClientSession().isClosing()) {
                reconnectSshSession(muzedoServer);
            }

            if (muzedoServer.getWsadminShell() != null
                && !muzedoServer.getWsadminShell().isClosing()
            ) {
                muzedoServer.getWsadminShell().close();
            }

            final var shell = sshService.createShellChannel(muzedoServer.getSshClientSession());
            shell.addCloseFutureListener((future) -> {
                log.warn(muzedoServer.getHost() + ": закрыт WsAdminShell!");
                reconnectWsadminShell(muzedoServer);
            });

            log.debug("{}: запускаю WsAdmin", muzedoServer.getHost());
            sshService.executeCommand(
                shell,
                commandDao.get("cd_root_deploy"),
                null);
            sshService.executeCommand(
                shell,
                commandDao.get("wsadmin_start"),
                null);

            muzedoServer.setWsadminShell(shell);
            log.debug("{}: WsAdmin запущен!", muzedoServer.getHost());
        } catch (Exception e) {
            log.error("#reconnectWsadminShell exception: {}", e.getMessage(), e);
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                reconnectWsadminShell(muzedoServer);
            }).start();
        } finally {
            muzedoServer.getWsadminConnectLock().unlock();
        }
    }
}
