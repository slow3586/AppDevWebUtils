package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.RequestHandler;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.command.Command;
import ru.blogic.muzedodevwebutils.server.MuzedoServer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class SSHService {
    private static final long TIMEOUT = 5000;
    private final SshClient client = SshClient.setUpDefaultClient();

    @Value("${app.p0}")
    private String p0;

    @Value("${app.p1}")
    private String p1;

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
            final var session = client.connect("root", muzedoServer.getHost(), 22)
                .verify(TIMEOUT)
                .getSession();

            session.addPasswordIdentity(muzedoServer.getP());
            session.auth().verify(TIMEOUT);

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
            final var channelShell = clientSession.createShellChannel();
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

    public String executeCommand(
        final ClientSession clientSession,
        final Command command,
        final AtomicInteger timerOut
    ) {
        try (final var channelShell = createShellChannel(clientSession)) {
            return executeCommand(channelShell, command, timerOut);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommand(
        final PtyCapableChannelSession channelShell,
        final Command command,
        final AtomicInteger timerOut
    ) {
        try (
            final var writer = new BufferedWriter(
                new OutputStreamWriter(
                    new NoCloseOutputStream(channelShell.getInvertedIn())));
            final var baos = new ByteArrayOutputStream()
        ) {
            channelShell.setOut(baos);

            channelShell.getInvertedIn().write(25);
            channelShell.getInvertedIn().flush();
            writer.write(command.command());
            writer.write(";\n");
            writer.flush();

            var count = 1;
            var substringBegin = 0;
            while (true) {
                Thread.sleep(1000);
                final var entireOutput = baos.toString().trim();
                final var newOutputPart = entireOutput.substring(substringBegin);
                if (!newOutputPart.isEmpty()) {
                    substringBegin += newOutputPart.length();
                    log.debug("{}: {}: ({}s) {}",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        count,
                        newOutputPart
                    );
                }
                final var err = command.errTexts()
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
                    throw new RuntimeException("#executeCommand ошибка: "
                        + channelShell.getSession().getRemoteAddress()
                        + ": " + err.get());
                }
                if (entireOutput.endsWith(command.readySymbol())) {
                    log.debug("{}: {}: complete @ {}s",
                        channelShell.getSession().getRemoteAddress(),
                        command.command(),
                        count);
                    return StringUtils.substringBeforeLast(entireOutput, "\n");
                }
                count++;
                if (timerOut != null) {
                    timerOut.set(count);
                }
                if (command.timeout() != 0 && count > command.timeout()) {
                    throw new RuntimeException("#executeCommand Таймаут: "
                        + channelShell.getSession().getRemoteAddress()
                        + ": " + command);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
