package ru.blogic.appdevwebutils.api.app.ssh;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ChannelShell;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Сущность SSH соединения, хранящая SSH канал и использующая Lock для хранения состояния использования.
 */
@Value
@Slf4j
public class SshConnection implements AutoCloseable {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    String id = String.valueOf(NEXT_ID.getAndIncrement());
    ReentrantLock beingUsedLock = new ReentrantLock();
    ChannelShell channelShell;

    /**
     * Соединение не закрывается, мы просто разрешаем использовать его заново, снимая ReentrantLock.
     */
    @Override
    public void close() {
        beingUsedLock.unlock();
        log.trace("#close " + channelShell.getSession().getRemoteAddress() + " #" + id);
    }
}
