package ru.blogic.appdevwebutils.api.app;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.api.app.ssh.SshService;
import ru.blogic.appdevwebutils.api.command.Command;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Сервис, отвечающий за поддержку SSH соединений с серверами приложений.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppServerService {
    SshService sshService;
    AppServerConfig appServerConfig;
    static ScheduledExecutorService executorService =
        Executors.newScheduledThreadPool(8);

    static Command COMMAND_CD_ROOT_DEPLOY = new Command("cd_root_deploy",
        "cd_root_deploy",
        Command.Shell.SSH,
        true,
        true,
        "cd /root/deploy/",
        Command.SSH_READY_PATTERN,
        10,
        true,
        List.empty());

    static Command COMMAND_WSADMIN_START = new Command("wsadmin_start",
        "wsadmin_start",
        Command.Shell.SSH,
        true,
        true,
        "./wsadmin_extra.sh",
        Command.WSADMIN_READY_PATTERN,
        60,
        true,
        Command.WSADMIN_ERR_PATTERNS);

    @Scheduled(fixedDelay = 1000 * 60 * 30, initialDelay = 0)
    public void scheduleSshSessionKeepAlive() {
        try {
            log.debug("#scheduleReconnect");
            appServerConfig.getAll()
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

    public void reconnectSshSession(AppServer appServer) {
        if (appServer.getSessionConnectLock().isLocked()
            || !appServer.getSessionConnectLock().tryLock()
        ) {
            log.warn("#reconnectSshSession {}: уже в процессе", appServer.host);
            return;
        }
        try {
            if (appServer.getSshClientSession() != null
                && appServer.getSshClientSession().isOpen()
            ) {
                appServer.getSshClientSession().close();
            }

            final ClientSession session = sshService.createSession(appServer);
            session.addCloseFutureListener((future) -> {
                log.warn(appServer.getHost() + ": закрыт SshSession!");
                reconnectSshSession(appServer);
            });

            appServer.setSshClientSession(session);
            appServer.setScpClient(sshService.createScpClient(session));
            log.debug(appServer.getHost() + ": SshSession установлена!");
        } catch (Exception e) {
            log.error("#reconnectSshSession exception: " + e.getMessage(), e);
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                reconnectSshSession(appServer);
            }).start();
            throw new RuntimeException(e);
        } finally {
            appServer.getSessionConnectLock().unlock();
        }

        reconnectWsadminShell(appServer);
    }

    public void reconnectWsadminShell(AppServer appServer) {
        if (appServer.getSessionConnectLock().isLocked()) {
            log.warn("#reconnectWsadminShell {}: reconnectSshSession в процессе", appServer.host);
            return;
        }
        if (appServer.getWsadminConnectLock().isLocked()
            || !appServer.getWsadminConnectLock().tryLock()) {
            log.warn("#reconnectWsadminShell {}: уже в процессе", appServer.host);
            return;
        }
        try {
            if (appServer.getSshClientSession() == null
                || !appServer.getSshClientSession().isOpen()) {
                reconnectSshSession(appServer);
            }

            if (appServer.getWsadminShell() != null
                && appServer.getWsadminShell().isOpen()
            ) {
                appServer.getWsadminShell().close();
            }

            final ChannelShell wsadminShell = sshService.createChannelShell(appServer.getSshClientSession());
            wsadminShell.addCloseFutureListener((future) -> {
                log.warn(appServer.getHost() + ": закрыт WsAdminShell!");
                reconnectWsadminShell(appServer);
            });

            log.debug("{}: запускаю WsAdmin", appServer.getHost());
            sshService.executeCommand(
                wsadminShell,
                COMMAND_CD_ROOT_DEPLOY,
                List.empty());
            sshService.executeCommand(
                wsadminShell,
                COMMAND_WSADMIN_START,
                List.empty());

            appServer.setWsadminShell(wsadminShell);
            log.debug("{}: WsAdmin запущен!", appServer.getHost());
        } catch (Exception e) {
            log.error("#reconnectWsadminShell exception: {}", e.getMessage(), e);
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                reconnectWsadminShell(appServer);
            }).start();
            throw new RuntimeException(e);
        } finally {
            appServer.getWsadminConnectLock().unlock();
        }
    }
}
