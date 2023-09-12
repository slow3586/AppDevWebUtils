package ru.blogic.muzedodevwebutils.api.file.logs.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileConfig {
    LogFileConfigExternalBinding logFileConfigExternalBinding;

    @NonFinal
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
