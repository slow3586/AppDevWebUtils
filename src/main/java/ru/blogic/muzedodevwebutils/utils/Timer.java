package ru.blogic.muzedodevwebutils.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;
import java.util.Set;

@Getter
@EqualsAndHashCode
public class Timer {
    protected int time = 0;
}
