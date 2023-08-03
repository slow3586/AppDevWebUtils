package ru.blogic.muzedodevwebutils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Data
@RequiredArgsConstructor
public class MuzedoServer {
    final int id;
    final String host;
    ClientSession clientSession;
    ChannelShell channelShell;
    AtomicReference<String> channelShellCurrentCommand = new AtomicReference<>("");
    boolean isGPRunning = false;
    boolean isIntegRunning = false;
    boolean isServerOn = false;
    List<InfoEntry> log = new ArrayList<>();
}
