package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
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
            throw new RuntimeException(e);
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
        try {
            return clientSession.executeRemoteCommand(command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String executeCommand(
        ChannelShell channelShell,
        String command,
        String readySymbol
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final var writer = new BufferedWriter(new OutputStreamWriter(channelShell.getInvertedIn()));
            channelShell.setOut(baos);
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
                    log.debug("{}: {}: {}",
                        channelShell.getSession().getRemoteAddress(),
                        command,
                        count);
                }
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
