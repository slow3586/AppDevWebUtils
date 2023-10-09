package ru.blogic.appdevwebutils.api.file.logs.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LogFileConfig {
    final LogFileConfigExternalBinding logFileConfigExternalBinding;

    List<LogFile> logFiles;

    @PostConstruct
    public void postConstruct() {
        logFiles = List.ofAll(
            logFileConfigExternalBinding.getList()
        ).map(configEntry ->
            new LogFile(
                configEntry.getId(),
                configEntry.getPath()));
    }

    public LogFile get(String id) {
        return logFiles
            .find(l -> l.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "#getLog: Не найден: " + id));
    }

    public List<LogFile> getAll() {
        return logFiles;
    }
}
