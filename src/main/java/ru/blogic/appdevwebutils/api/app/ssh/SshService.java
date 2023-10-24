package ru.blogic.appdevwebutils.api.app.ssh;

import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.api.app.AppServer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SshService {
    private static final int MAX_CONNECTION_POOL_SIZE = 5;
    static long DEFAULT_TIMEOUT = 5000;
    static String COMMAND_OUTPUT_START = "_OUTPUT_";
    static ScpClientCreator SCP_CLIENT_CREATOR = ScpClientCreator.instance();
    static SshClient DEFAULT_SSH_CLIENT = SshClient.setUpDefaultClient();
    @NonFinal
    @Value("${ssh-service.username:root}")
    String username;
    @NonFinal
    @Value("${ssh-service.port:22}")
    String port;

    @PostConstruct
    public void postConstruct() {
        try {
            DEFAULT_SSH_CLIENT.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Создает SSH сессию для указанного сервера. */
    public ClientSession createSession(final AppServer appServer) {
        try {
            final ClientSession session = DEFAULT_SSH_CLIENT.connect(
                    username,
                    appServer.getHost(),
                    Try.of(() -> Integer.parseInt(port))
                        .onFailure((e) -> log.error("#createSession Некорректный порт SSH", e))
                        .getOrElse(22)
                ).verify(DEFAULT_TIMEOUT)
                .getSession();

            session.addPasswordIdentity(appServer.getPassword());
            session.auth().verify(DEFAULT_TIMEOUT);

            return session;
        } catch (Exception e) {
            throw new RuntimeException(
                "#createSession " + appServer.getHost() + " Не удалось создать SSH сессию: " + e.getMessage(), e);
        }
    }

    /** Создает SSH канал для указанной SSH сессии.  */
    public ChannelShell createChannelShell(final ClientSession clientSession) {
        try {
            final ChannelShell channelShell = clientSession.createShellChannel();
            channelShell.setRedirectErrorStream(true);
            channelShell.open().verify(DEFAULT_TIMEOUT);
            channelShell.addRequestHandler((channel, request, wantReply, buffer) -> {
                if (StringUtils.contains(request, "keepalive@openssh.com"))
                    return RequestHandler.Result.ReplySuccess;
                return RequestHandler.Result.Unsupported;
            });
            return channelShell;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Предоставляет SSH соединение для сервера приложения, при необходимости создавая новое в пуле соединений. */
    public SshConnection getSshConnection(final AppServer appServer) {
        log.debug("#createShellChannel " + appServer.getId());
        final java.util.List<SshConnection> connectionPool = appServer.getSshConnectionPool();

        final ReentrantLock poolLock = appServer.getSshConnectionPoolLock();
        int attempt = 0;
        while (poolLock.isLocked() || !poolLock.tryLock()) {
            if (attempt++ > 10) {
                throw new RuntimeException("#createShellChannel attempt > 10");
            }
            log.warn("#createShellChannel poolLock locked");
            Try.run(() -> Thread.sleep(1000));
        }

        try {
            return List.ofAll(connectionPool)
                .find(connection -> connection.getChannelShell().isOpen()
                    && !connection.getBeingUsedLock().isLocked()
                    && connection.getBeingUsedLock().tryLock())
                .getOrElse(() -> Try.of(() -> {
                        log.debug("#createShellChannel нет свободных SSHConnection, создаю новое");
                        if (connectionPool.size() >= MAX_CONNECTION_POOL_SIZE) {
                            log.warn("#createShellChannel превышен лимит SSHConnection");
                            Thread.sleep(5000);
                            return getSshConnection(appServer);
                        }
                        final ChannelShell channelShell = createChannelShell(appServer.getSshClientSession());

                        final SshConnection newConnection = new SshConnection(channelShell);
                        newConnection.getBeingUsedLock().lock();
                        connectionPool.add(newConnection);
                        channelShell.addCloseFutureListener((closeFuture ->
                            connectionPool.remove(newConnection)));
                        return newConnection;
                    }).peek(connection -> log.trace(
                        "#createShellChannel взял connection #" + connection.getId()))
                    .get());
        } finally {
            poolLock.unlock();
        }
    }

    /** Создает SCP клиент для указанной SSH сессии. */
    public ScpClient createScpClient(final ClientSession clientSession) {
        return SCP_CLIENT_CREATOR.createScpClient(clientSession);
    }

    /** Выполняет операцию через SSH сервера, при необходимости создавая новый SSH канал. */
    public String executeCommand(
        final AppServer appServer,
        final Command command,
        final List<String> arguments
    ) {
        try (final SshConnection sshConnection = getSshConnection(appServer)) {
            return executeCommand(sshConnection.getChannelShell(), command, arguments);
        }
    }

    /** Выполняет операцию в указанном SSH канале. */
    public String executeCommand(
        @NonNull final PtyCapableChannelSession channelShell,
        @NonNull final Command command,
        @NonNull final List<String> arguments
    ) {
        try (
            final OutputStream input = new NoCloseOutputStream(channelShell.getInvertedIn());
            final ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            input.write(21);
            input.flush();

            final String commandText =
                List.of(
                        (command.shell() == Command.Shell.SSH ? "echo '" + COMMAND_OUTPUT_START + "' &&" : ""),
                        command.command(),
                        arguments.mkString(" ")
                    ).filter(StringUtils::isNotBlank)
                    .mkString(" ");
            log.debug("commandText: " + commandText);
            input.write(commandText.getBytes());
            input.write("\n".getBytes());
            input.flush();

            channelShell.setOut(output);

            int substringBegin = 0;
            int timer = 0;
            while (true) {
                Thread.sleep(1000);
                timer++;
                final String outputString = output.toString().trim();
                final String newOutputPart = outputString.substring(substringBegin);
                if (!newOutputPart.isEmpty()) {
                    substringBegin += newOutputPart.length();
                    log.debug("{}: {}: ({}s) {}",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        timer,
                        newOutputPart
                    );
                }
                final Option<String> err = command.errPatterns()
                    .find(e ->
                        StringUtils.containsIgnoreCase(
                            outputString, e));
                if (!err.isEmpty()) {
                    log.error("{}: {}: обнаружена ошибка \"{}\"",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        err.get());
                    throw new RuntimeException("Ошибка: "
                        + channelShell.getSession().getRemoteAddress()
                        + ": " + err.get());
                }
                if (StringUtils.isBlank(command.readyPattern())
                    || outputString.endsWith(command.readyPattern())
                ) {
                    log.debug("{}: {}: complete @ {}s",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        timer);

                    return StringUtils.substringAfterLast(
                        StringUtils.substringBeforeLast(outputString, "\r\n"),
                        COMMAND_OUTPUT_START + "\r\n"
                    );
                }
                if (command.timeout() != 0 && timer > command.timeout()) {
                    throw new RuntimeException("Достигнут таймаут "
                        + "(" + command.timeout() + " сек.)");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("#executeCommand: " + e.getMessage(), e);
        }
    }

    /** Скачивает файл через SCP сервера приложения по указанному пути. */
    public byte[] downloadFile(
        @NonNull final AppServer appServer,
        @NonNull final String path
    ) {
        try {
            return appServer.getScpClient().downloadBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("#downloadFile: " + e.getMessage(), e);
        }
    }

    /** Загружает файл через SCP сервера приложения по указанному пути. */
    public void uploadFile(
        @NonNull final AppServer appServer,
        final byte[] data,
        @NonNull final String path
    ) {
        try {
            final long now = new Date().getTime();
            appServer.getScpClient().upload(
                data,
                path,
                java.util.List.of(
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_READ),
                new ScpTimestampCommandDetails(now, now));
        } catch (Exception e) {
            throw new RuntimeException("#downloadFile: " + e.getMessage(), e);
        }
    }
}
