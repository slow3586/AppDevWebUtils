package ru.blogic.muzedodevwebutils.api.file.logs;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.file.logs.config.LogFile;
import ru.blogic.muzedodevwebutils.api.file.logs.config.LogFileConfig;
import ru.blogic.muzedodevwebutils.api.file.logs.dto.GetLogFileRequest;
import ru.blogic.muzedodevwebutils.api.file.logs.dto.GetLogFileResponse;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.config.MuzedoServerConfig;
import ru.blogic.muzedodevwebutils.api.muzedo.ssh.SSHService;
import ru.blogic.muzedodevwebutils.utils.Utils;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileService {
    MuzedoServerConfig muzedoServerConfig;
    SSHService sshService;
    LogFileConfig logFileConfig;
    WebClient client;

    static DateTimeFormatter DATE_FORMAT_LOG_FILE = DateTimeFormatter.ofPattern(
        "yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH);

    public LogFileService(
        MuzedoServerConfig muzedoServerConfig,
        SSHService sshService,
        LogFileConfig logFileConfig,
        WebClient.Builder webClientBuilder
    ) {
        this.muzedoServerConfig = muzedoServerConfig;
        this.sshService = sshService;
        this.logFileConfig = logFileConfig;
        this.client = webClientBuilder.build();
    }

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
        "zip -9 -j -q -", //-9 уровень сжатия, -j без папок, -q без лишней инфы
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );

    public String getServerLogFile(
        final GetLogFileRequest request
    ) {
        final MuzedoServer muzedoServer = muzedoServerConfig.get(request.serverId());
        final LogFile serverLog = logFileConfig.get(request.logId());

        final int lineCount = Utils.clamp(request.linesCount(), 1, 1000);

        return sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            COMMAND_TAIL,
            List.of(
                "-n " + lineCount,
                muzedoServer.getFilePaths().logsFilePath()
                    + "/"
                    + serverLog.path()));
    }

    public ResponseEntity<Resource> getEntireLogFile(int serverId, String logId) {
        final MuzedoServer muzedoServer = muzedoServerConfig.get(serverId);
        final LogFile serverLog = logFileConfig.get(logId);

        final String result = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            COMMAND_ZIP,
            List.of(
                muzedoServer.getFilePaths().logsFilePath() + "/" + serverLog.path(),
                "| base64"
            ));
            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set(
                HttpHeaders.CONTENT_DISPOSITION,
                serverId
                + "_" + serverLog.id()
                + "_" + DATE_FORMAT_LOG_FILE.format(LocalDateTime.now())
                + ".zip");
            final byte[] decoded = Base64.getDecoder().decode(
                StringUtils.replace(result, "\r\n", ""));
            return new ResponseEntity<>(
                new ByteArrayResource(decoded),
                responseHeaders,
                HttpStatus.OK);
    }

    public Mono<ResponseEntity<Resource>> getLogsArchive(int serverId) {
        final MuzedoServer muzedoServer = muzedoServerConfig.get(serverId);

        return client.get()
            .uri("http://" + muzedoServer.getHost() + "/log/archive")
            .retrieve()
            .toBodilessEntity()
            .doOnError(e -> log.error("Ошибка запроса архива логов", e))
            .onErrorResume(Mono::error)
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
}
