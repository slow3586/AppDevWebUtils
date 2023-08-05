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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
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
            throw new RuntimeException("#createSession Не удалось создать SSH сессию: " + e.getMessage(), e);
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
                if ("keepalive@openssh.com".equals(request))
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
        String command
    ) {
        try (ChannelShell channelShell = createShellChannel(clientSession)) {
            return executeCommand(channelShell, command, "#");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommand(
        PtyCapableChannelSession channelShell,
        String command,
        String readySymbol
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            channelShell.setOut(baos);

            final var writer = new BufferedWriter(new OutputStreamWriter(channelShell.getInvertedIn()));
            writer.write(command);
            writer.write("\n");
            writer.flush();

            int count = 0;
            while (true) {
                Thread.sleep(1000);
                final var string = baos.toString().trim();
                if (string.endsWith(readySymbol)) {
                    return StringUtils.substringBeforeLast(string, "\n");
                }
                if (count != 0 && count % 5 == 0) {
                    log.debug("{}: {}: {} {}",
                        channelShell.getSession().getRemoteAddress(),
                        command,
                        count,
                        (count >= 60 && count % 60 == 0)
                            ? "\n" + string : ""
                    );
                }
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
