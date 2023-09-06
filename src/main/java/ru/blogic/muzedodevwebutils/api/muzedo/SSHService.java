package ru.blogic.muzedodevwebutils.api.muzedo;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.ChannelListener;
import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SSHService {
    static long TIMEOUT = 5000;
    SshClient client = SshClient.setUpDefaultClient();

    @PostConstruct
    public void postConstruct() {
        try {
            client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClientSession createSession(
        final MuzedoServer muzedoServer
    ) {
        try {
            final ClientSession session = client.connect("root", muzedoServer.getHost(), 22)
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

    public Mono<ExecuteCommandResult> executeCommand(
        final ClientSession clientSession,
        final Command command,
        final List<String> arguments
    ) {
        try (val channelShell = createShellChannel(clientSession)) {
            return executeCommand(channelShell, command, arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record ExecuteCommandResult(
        String entireOutput,
        String commandOutput
    ) {}

    public Mono<ExecuteCommandResult> executeCommand(
        @NonNull final PtyCapableChannelSession channelShell,
        @NonNull final Command command,
        @NonNull final List<String> arguments
    ) {
        try (
            val in = new NoCloseOutputStream(channelShell.getInvertedIn());
            final ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            in.write(21);
            in.flush();

            channelShell.setOut(baos);

            final String commandText = List.of(
                command.command(),
                arguments.mkString(" ")
            ).mkString(" ");
            log.debug("commandText: " + commandText);
            in.write(commandText.getBytes());
            in.write("\n".getBytes());
            in.flush();

            var substringBegin = 0;
            var timer = 0;
            while (true) {
                Thread.sleep(1000);
                timer++;
                final String entireOutput = baos.toString().trim();
                final String newOutputPart = entireOutput.substring(substringBegin);
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
                            entireOutput, e))
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
                    || entireOutput.endsWith(command.readyPattern())
                ) {
                    log.debug("{}: {}: complete @ {}s",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        timer);

                    final String commandOutputResult =
                        StringUtils.substringAfter(
                            StringUtils.substringAfter(
                                StringUtils.substringAfter(
                                    StringUtils.substringBeforeLast(
                                        entireOutput,
                                        "\n"),
                                    commandText),
                                "\n"),
                            "\n");

                    return Mono.just(new ExecuteCommandResult(
                        entireOutput,
                        commandOutputResult
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
}
