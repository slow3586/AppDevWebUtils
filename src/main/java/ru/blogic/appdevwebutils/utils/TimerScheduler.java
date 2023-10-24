package ru.blogic.appdevwebutils.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Позволяет создавать таймеры, отслеживающее время с момента их старта.
 */
@Service
public class TimerScheduler {
    /**
     * Список запущенных таймеров.
     */
    private static final Set<Timer> timers = new HashSet<>();

    /**
     * Запланированная задача отвечает за отслеживание времени с момента старта.
     */
    @Scheduled(fixedRate = 1000)
    protected static void updateTimers() {
        timers.forEach(t -> t.time += 1);
    }

    /**
     * Запускает новый таймер, начиная отслеживание времени.
     */
    public Timer start() {
        final Timer timer = new Timer();
        timers.add(timer);
        return timer;
    }

    /**
     * Останавливает указанный таймер.
     */
    public void stop(Timer timer) {
        timers.remove(timer);
    }

    /**
     * Сущность таймера.
     */
    @Getter
    @EqualsAndHashCode
    final public static class Timer {
        private int time = 0;
    }
}
