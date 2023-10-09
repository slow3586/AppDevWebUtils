package ru.blogic.appdevwebutils.utils;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class TimerScheduler {
    private static final Set<Timer> timers = new HashSet<>();

    @Scheduled(fixedRate = 1000)
    protected static void updateTimers() {
        timers.forEach(t -> t.time += 1);
    }

    public Timer start() {
        final Timer timer = new Timer();
        timers.add(timer);
        return timer;
    }

    public void stop(Timer timer) {
        timers.remove(timer);
    }
}
