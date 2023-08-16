package ru.blogic.muzedodevwebutils.api.file.logs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.muzedodevwebutils.api.info.InfoService;

@RestController
@RequestMapping("api/file/log")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileServiceRest {
    LogFileService logFileService;

    @GetMapping(path = "{serverId}", produces = "application/json")
    public LogFileService.GetLogFileResponse getServerLog(
        @PathVariable final int serverId
    ) {
        return logFileService.getServerLog(serverId);
    }
}
