package ru.blogic.muzedodevwebutils;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.ChannelListener;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.EnumSet;

public class CommandServiceTest {

    public ClientSession auth() throws IOException {
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ClientSession session = client.connect("root", "172.19.203.61", 22)
            .verify(1000)
            .getSession();
        session.addPasswordIdentity("");
        session.auth().verify(1000);

        return session;
    }

    @Test
    public void shellTest() throws IOException, InterruptedException {
        try (ClientSession session = auth()) {
            try (ChannelShell channelShell = session.createShellChannel()) {
                channelShell.setRedirectErrorStream(true);
                channelShell.open().verify(1000);

                Thread t = new Thread(() -> {
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(channelShell.getInvertedIn()))) {
                        for (int i = 0; i < 10; i++) {
                            writer.write("ls\n");
                            writer.flush();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            channelShell.setOut(baos);
                            while (true) {
                                Thread.sleep(1000);
                                String string = baos.toString();
                                System.out.println(string);
                                if (string.endsWith("# ")) {
                                    System.out.println("OK!!!");
                                    break;
                                }
                                System.out.println("NOT YET!!!");
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                t.start();

                channelShell.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), -1);

                System.out.println("CLOSED!!!");
            }

        }
    }

    @Test
    public void qwerr() throws IOException {
        try (ClientSession session = auth()) {
            String ls = session.executeRemoteCommand("ls");
            System.out.println(ls);
        }
    }
}
