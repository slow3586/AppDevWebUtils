package ru.blogic.muzedodevwebutils.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;
import java.util.Set;

@Getter
@EqualsAndHashCode
public class Timer implements AutoCloseable {
    private static final Set<Timer> timers = new HashSet<>();
    private int time = 0;

    public Timer start() {
        time = 0;
        timers.add(this);
        return this;
    }

    @Scheduled(fixedDelay = 1000)
    protected static void updateTimers() {
        timers.forEach(t -> t.time += 1);
    }

    @Override
    public void close() throws Exception {
        timers.remove(this);
    }
}
