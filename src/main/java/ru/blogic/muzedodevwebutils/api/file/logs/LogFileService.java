package ru.blogic.muzedodevwebutils.api.file.logs;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.file.logs.dao.LogFileDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.ssh.SSHService;
import ru.blogic.muzedodevwebutils.utils.Utils;

import java.net.URI;
import java.util.Base64;
import java.util.Objects;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileService {
    MuzedoServerDao muzedoServerDao;
    SSHService sshService;
    LogFileDao logFileDao;
    WebClient client;

    public LogFileService(
        MuzedoServerDao muzedoServerDao,
        SSHService sshService,
        LogFileDao logFileDao,
        WebClient.Builder webClientBuilder
    ) {
        this.muzedoServerDao = muzedoServerDao;
        this.sshService = sshService;
        this.logFileDao = logFileDao;
        this.client = webClientBuilder.build();
    }

    // ПУТЬ К ФАЙЛУ НЕ ДОЛЖЕН БЫТЬ ДЛИННЫМ, ИНАЧЕ SSH БУДЕТ ВСТАВЛЯТЬ ЛЕВЫЕ СИМВОЛЫ
    // В ТАКОМ СЛУЧАЕ НУЖНО ИСПОЛЬЗОВАТЬ SYMLINKИ
    static Command COMMAND_TAIL = new Command(
        "tail",
        "Tail",
        Command.Shell.SSH,
        false,
        true,
        "tail",
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );

    static Command COMMAND_ZIP = new Command(
        "zip",
        "zip",
        Command.Shell.SSH,
        false,
        true,
        "zip -9 -j -",
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );

    public Mono<GetLogFileResponse> getServerLogFile(
        final GetLogFileRequest request
    ) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(request.serverId());
        final LogFile serverLog = logFileDao.get(request.logId());

        final int lineCount = Utils.clamp(request.linesCount(), 1, 1000);

        final Mono<SSHService.ExecuteCommandResult> response = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            COMMAND_TAIL,
            List.of(
                "-n " + lineCount,
                muzedoServer.getFilePaths().logsFilePath()
                    + "/"
                    + serverLog.path()));

        return response.map(r ->
            new GetLogFileResponse(r.commandOutput().mkString("\n")));
    }

    public Mono<ResponseEntity<Resource>> getEntireLogFile(int serverId, String logId) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(serverId);
        final LogFile serverLog = logFileDao.get(logId);

        return sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            COMMAND_ZIP,
            List.of(
                muzedoServer.getFilePaths().logsFilePath() + "/" + serverLog.path(),
                "| base64"
            )
        ).map(response -> {
            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, serverLog.id() + ".zip");
            final byte[] decoded = Base64.getDecoder().decode(
                response.commandOutput().drop(4).
                    mkString(""));
            return new ResponseEntity<>(
                new ByteArrayResource(decoded),
                responseHeaders,
                HttpStatus.OK);
        });
    }

    public Mono<ResponseEntity<Resource>> getLogsArchive(int serverId) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(serverId);

        return client.get()
            .uri("http://" + muzedoServer.getHost() + "/log/archive")
            .retrieve()
            .toBodilessEntity()
            .doOnError(e -> log.error("Ошибка запроса архива логов", e))
            .mapNotNull(HttpEntity::getHeaders)
            .mapNotNull(HttpHeaders::getLocation)
            .mapNotNull(URI::toString)
            .filter(StringUtils::isNotBlank)
            .mapNotNull(o -> StringUtils.replace(o, "link.jsp", "download"))
            .flatMap(link -> client.get()
                .uri(link)
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(byte[].class)
                .map(ByteArrayResource::new)
                .map(r -> {
                    final HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                        StringUtils.substringAfter(link, "?"));
                    return new ResponseEntity<>(
                        r,
                        responseHeaders,
                        HttpStatus.OK);
                }));
    }

    public record GetLogFileRequest(
        int serverId,
        String logId,
        int linesCount
    ) {}

    public record GetLogFileResponse(
        String text
    ) {}
}
