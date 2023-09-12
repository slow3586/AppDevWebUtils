package ru.blogic.muzedodevwebutils.api.muzedo.ssh;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.ChannelListener;
import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.apache.sshd.scp.client.CloseableScpClient;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SSHService {
    static long TIMEOUT = 5000;
    ScpClientCreator SCP_CLIENT_CREATOR = ScpClientCreator.instance();
    SshClient DEFAULT_SSH_CLIENT = SshClient.setUpDefaultClient();

    @PostConstruct
    public void postConstruct() {
        try {
            DEFAULT_SSH_CLIENT.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClientSession createSession(
        final MuzedoServer muzedoServer
    ) {
        try {
            final ClientSession session = DEFAULT_SSH_CLIENT.connect("root", muzedoServer.getHost(), 22)
                .verify(TIMEOUT)
                .getSession();

            session.addPasswordIdentity(muzedoServer.getPassword());
            session.auth().verify(TIMEOUT);
            session.addChannelListener(new ChannelListener() {});

            return session;
        } catch (Exception e) {
            throw new RuntimeException(
                "#createSession " + muzedoServer.getHost() + " Не удалось создать SSH сессию: " + e.getMessage(), e);
        }
    }

    public ChannelShell createShellChannel(
        final ClientSession clientSession
    ) {
        try {
            final ChannelShell channelShell = clientSession.createShellChannel();
            channelShell.setRedirectErrorStream(true);
            channelShell.open().verify(TIMEOUT);
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

    public ScpClient createScpClient(
        final ClientSession clientSession
    ) {
        return SCP_CLIENT_CREATOR.createScpClient(clientSession);
    }

    public Mono<ExecuteCommandResult> executeCommand(
        final ClientSession clientSession,
        final Command command,
        final List<String> arguments
    ) {
        try (ChannelShell channelShell = createShellChannel(clientSession)) {
            return executeCommand(channelShell, command, arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record ExecuteCommandResult(
        String commandOutput
    ) {}

    public Mono<ExecuteCommandResult> executeCommand(
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
                        (command.shell() == Command.Shell.SSH ? "echo '_COMMAND_OUTPUT_START_' &&" : ""),
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
                final Optional<String> err = command.errPatterns()
                    .stream()
                    .filter(e ->
                        StringUtils.containsIgnoreCase(
                            outputString, e))
                    .findFirst();
                if (err.isPresent()) {
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

                    return Mono.just(new ExecuteCommandResult(
                        StringUtils.substringAfterLast(
                            StringUtils.substringBeforeLast(outputString, "\r\n"),
                            "_COMMAND_OUTPUT_START_\r\n"
                        )
                    ));
                }
                if (command.timeout() != 0 && timer > command.timeout()) {
                    throw new RuntimeException("Таймаут: "
                        + channelShell.getSession().getRemoteAddress()
                        + ": " + command.name());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("#executeCommand: " + e.getMessage(), e);
        }
    }

    public byte[] downloadFile(
        @NonNull final MuzedoServer muzedoServer,
        @NonNull final String path
    ) {
        try {
            return muzedoServer.getScpClient().downloadBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("#downloadFile: " + e.getMessage(), e);
        }
    }

    public void uploadFile(
        @NonNull final MuzedoServer muzedoServer,
        final byte[] data,
        @NonNull final String path
    ) {
        try {
            final long now = new Date().getTime();
            muzedoServer.getScpClient().upload(
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
