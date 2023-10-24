package ru.blogic.appdevwebutils.api.file.logs.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Конфигурация сервиса предоставления файлов логов, хранящихся на серверах приложений.
 */
@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class LogFileConfig {
    /**
     * Внешняя конфигурация
     */
    final LogFileConfigExternalBinding logFileConfigExternalBinding;

    List<LogFile> logFiles;
    final DateTimeFormatter dateFormatLogFile = DateTimeFormatter.ofPattern(
        "yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH);

    /**
     * Конвертация внешней конфигурации.
     */
    @PostConstruct
    public void postConstruct() {
        logFiles = List.ofAll(
            logFileConfigExternalBinding.getList()
        ).map(configEntry ->
            new LogFile(
                configEntry.getId(),
                configEntry.getPath()));
    }

    /**
     * Предоставляет информацию о файле лога с указанным ID.
     */
    public LogFile get(String id) {
        return logFiles
            .find(l -> l.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "#getLog: Не найден: " + id));
    }
}
