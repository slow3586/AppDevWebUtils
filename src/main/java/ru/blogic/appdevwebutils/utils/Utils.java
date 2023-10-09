package ru.blogic.appdevwebutils.utils;

import io.vavr.collection.List;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public class Utils {
    public static int clamp(int in, int min, int max) {
        return Math.min(Math.max(in, min), max);
    }

    public static List<String> splitByLines(String text) {
        return List.ofAll(StringUtils.defaultString(text).lines());
    }
}
