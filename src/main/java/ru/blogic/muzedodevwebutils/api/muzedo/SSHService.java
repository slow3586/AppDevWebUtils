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
            val session = client.connect("root", muzedoServer.getHost(), 22)
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
            val channelShell = clientSession.createShellChannel();
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
            val baos = new ByteArrayOutputStream()
        ) {
            channelShell.setOut(baos);

            val commandText = List.of(
                command.command(),
                arguments.mkString(" ")
            ).mkString(" ");

            in.write(21);
            in.write(commandText.getBytes());
            in.write("\n".getBytes());
            in.flush();

            var substringBegin = 0;
            var timer = 0;
            while (true) {
                Thread.sleep(1000);
                timer++;
                val entireOutput = baos.toString().trim();
                val newOutputPart = entireOutput.substring(substringBegin);
                if (!newOutputPart.isEmpty()) {
                    substringBegin += newOutputPart.length();
                    log.debug("{}: {}: ({}s) {}",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        timer,
                        newOutputPart
                    );
                }
                val err = command.errPatterns()
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
                if (entireOutput.endsWith(command.readyPattern())) {
                    log.debug("{}: {}: complete @ {}s",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        timer);

                    val entireOutputResult = StringUtils.substringBeforeLast(entireOutput, "\n");
                    val commandOutputResult = StringUtils.substringAfter(
                            StringUtils.substringAfter(
                                entireOutputResult,
                                commandText),
                            "\n");

                    return Mono.just(new ExecuteCommandResult(
                        entireOutputResult,
                        commandOutputResult
                    ));
                }
                if (command.timeout() != 0 && timer > command.timeout()) {
                    throw new RuntimeException("Таймаут: "
                        + channelShell.getSession().getRemoteAddress()
                        + ": " + command);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("#executeCommand: " + e.getMessage(), e);
        }
    }
}
