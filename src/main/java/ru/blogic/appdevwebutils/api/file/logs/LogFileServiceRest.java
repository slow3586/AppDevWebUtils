package ru.blogic.appdevwebutils.api.file.logs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.blogic.appdevwebutils.api.file.logs.dto.GetLogFileRequest;

@RestController
@RequestMapping("api/file/log")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileServiceRest {
    LogFileService logFileService;

    @PostMapping(produces = "application/json")
    public String getServerLogFile(
        @RequestBody final GetLogFileRequest getLogFileRequest
    ) {
        return logFileService.getServerLogFile(getLogFileRequest);
    }

    @GetMapping(path = "getEntireLogFile/{serverId}/{logId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getEntireLogFile(
        @PathVariable final int serverId,
        @PathVariable final String logId
    ) {
        return logFileService.getEntireLogFile(serverId, logId);
    }

    @GetMapping(path = "getLogsArchive/{serverId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Mono<ResponseEntity<Resource>> getLogsArchive(
        @PathVariable final int serverId
    ) {
        return logFileService.getLogsArchive(serverId);
    }
}
