package ru.blogic.muzedodevwebutils.api.file.logs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandRunRequest;

@RestController
@RequestMapping("api/file/log")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileServiceRest {
    LogFileService logFileService;

    @PostMapping(produces = "application/json")
    public Mono<LogFileService.GetLogFileResponse> getServerLogFile(
        @RequestBody final LogFileService.GetLogFileRequest getLogFileRequest
    ) {
        return logFileService.getServerLogFile(getLogFileRequest);
    }
}
