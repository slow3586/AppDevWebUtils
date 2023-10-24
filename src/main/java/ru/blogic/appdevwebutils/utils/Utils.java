package ru.blogic.appdevwebutils.utils;

import io.vavr.collection.List;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Вспомогательные функции.
 */
public class Utils {
    /**
     * Возвращает число, поместив его в указанные границы.
     * 5, 1, 2 -> 2
     * 0, 1, 2 -> 1
     */
    public static int clamp(int in, int min, int max) {
        return Math.min(Math.max(in, min), max);
    }

    /**
     * Разделяет текст на строки.
     * "текст\nтекст" -> ["текст", "текст"]
     * null -> ""
     */
    public static List<String> splitByLines(String text) {
        return List.ofAll(StringUtils.defaultString(text).lines());
    }

    /** Возвращает самую раннюю возможную дату */
    public static ZonedDateTime getZeroDate(){
        return ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
    }
}
