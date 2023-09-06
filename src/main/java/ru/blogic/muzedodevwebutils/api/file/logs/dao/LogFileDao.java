package ru.blogic.muzedodevwebutils.api.file.logs.dao;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.api.file.logs.LogFile;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.List;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileDao {
    LogFileConfig logFileConfig;
    List<LogFile> logFiles = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        logFiles.addAll(logFileConfig.getList()
            .stream()
            .map(c -> new LogFile(
                c.getId(),
                c.getPath()
            )).toList()
        );
    }

    public LogFile get(String id) {
        return io.vavr.collection.List.ofAll(logFiles)
            .find(l -> l.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "#getLog: Не найден: " + id));
    }

    public io.vavr.collection.List<LogFile> getAll() {
        return io.vavr.collection.List.ofAll(logFiles);
    }
}
