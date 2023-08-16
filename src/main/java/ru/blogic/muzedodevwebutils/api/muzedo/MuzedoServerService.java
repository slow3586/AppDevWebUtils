package ru.blogic.muzedodevwebutils.api.muzedo;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.command.CommandDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MuzedoServerService {
    CommandDao commandDao;
    SSHService sshService;
    MuzedoServerDao muzedoServerDao;
    ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(8);

    @Scheduled(fixedDelay = 1000 * 60 * 30, initialDelay = 0)
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
        if (muzedoServer.getSessionConnectLock().isLocked()
            || !muzedoServer.getSessionConnectLock().tryLock()) {
            log.warn("#reconnectSshSession {}: уже в процессе", muzedoServer.host);
            return;
        }
        try {
            if (muzedoServer.getSshClientSession() != null
                && muzedoServer.getSshClientSession().isOpen()
            ) {
                muzedoServer.getSshClientSession().close();
            }

            val session = sshService.createSession(muzedoServer);
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
            throw new RuntimeException(e);
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
        if (muzedoServer.getWsadminConnectLock().isLocked()
            || !muzedoServer.getWsadminConnectLock().tryLock()) {
            log.warn("#reconnectWsadminShell {}: уже в процессе", muzedoServer.host);
            return;
        }
        try {
            if (muzedoServer.getSshClientSession() == null
                || !muzedoServer.getSshClientSession().isOpen()) {
                reconnectSshSession(muzedoServer);
            }

            if (muzedoServer.getWsadminShell() != null
                && muzedoServer.getWsadminShell().isOpen()
            ) {
                muzedoServer.getWsadminShell().close();
            }

            val shell = sshService.createShellChannel(muzedoServer.getSshClientSession());
            shell.addCloseFutureListener((future) -> {
                log.warn(muzedoServer.getHost() + ": закрыт WsAdminShell!");
                reconnectWsadminShell(muzedoServer);
            });

            log.debug("{}: запускаю WsAdmin", muzedoServer.getHost());
            sshService.executeCommand(
                shell,
                commandDao.get("cd_root_deploy"),
                List.empty(),
                null);
            sshService.executeCommand(
                shell,
                commandDao.get("wsadmin_start"),
                List.empty(),
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
            throw new RuntimeException(e);
        } finally {
            muzedoServer.getWsadminConnectLock().unlock();
        }
    }
}
