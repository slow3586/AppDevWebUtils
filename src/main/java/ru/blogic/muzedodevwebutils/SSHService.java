package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.RequestHandler;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.command.Command;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class SSHService {

    private static final long TIMEOUT = 5000;

    SshClient client;

    @PostConstruct
    public void postConstruct() {
        try {
            client = SshClient.setUpDefaultClient();
            client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ClientSession createSession(
        String host
    ) {
        try {
            final var session = client.connect("root", host, 22)
                .verify(TIMEOUT)
                .getSession();
            session.addPasswordIdentity("");
            session.auth().verify(TIMEOUT);

            return session;
        } catch (Exception e) {
            throw new RuntimeException(
                "#createSession " + host + " Не удалось создать SSH сессию: " + e.getMessage(), e);
        }
    }

    public ChannelShell createShellChannel(
        ClientSession clientSession
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
        ClientSession clientSession,
        Command command
    ) {
        try (ChannelShell channelShell = createShellChannel(clientSession)) {
            return executeCommand(channelShell, command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommand(
        PtyCapableChannelSession channelShell,
        Command command
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final var outputParts = new ArrayList<String>();
            channelShell.setOut(baos);

            final var writer = new BufferedWriter(new OutputStreamWriter(channelShell.getInvertedIn()));
            writer.write(command.command());
            writer.write("\n");
            writer.flush();

            var count = 0;
            while (true) {
                Thread.sleep(1000);
                final var entireOutput = baos.toString().trim();
                final var newOutputPart = entireOutput.substring(outputParts
                    .stream()
                    .mapToInt(String::length)
                    .sum());
                if (!newOutputPart.isEmpty()) {
                    outputParts.add(newOutputPart);
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
                    throw new RuntimeException("#executeCommand обнаружен текст ошибки: "
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
