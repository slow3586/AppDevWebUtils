package ru.blogic.appdevwebutils.api.file.logs;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
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
import ru.blogic.appdevwebutils.api.file.logs.config.LogFile;
import ru.blogic.appdevwebutils.api.file.logs.config.LogFileConfig;
import ru.blogic.appdevwebutils.api.file.logs.dto.GetLogFileRequest;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.api.app.ssh.SshService;
import ru.blogic.appdevwebutils.utils.Utils;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Сервис, отвечающий за предоставление файлов логов, хранящихся на серверах приложений.
 */
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileService {
    AppServerConfig appServerConfig;
    SshService sshService;
    LogFileConfig logFileConfig;
    WebClient client;

    /**
     * Конструктор необходим для webClientBuilder.
     */
    protected LogFileService(
        AppServerConfig appServerConfig,
        SshService sshService,
        LogFileConfig logFileConfig,
        WebClient.Builder webClientBuilder
    ) {
        this.appServerConfig = appServerConfig;
        this.sshService = sshService;
        this.logFileConfig = logFileConfig;
        this.client = webClientBuilder.build();
    }

    /**
     * Предоставляет указанное количество строк с конца указанного лог-файла.
     */
    public String getServerLogFile(
        final GetLogFileRequest request
    ) {
        final AppServer appServer = appServerConfig.get(request.serverId());
        final LogFile serverLog = logFileConfig.get(request.logId());

        final int lineCount = Utils.clamp(request.linesCount(), 1, 1000);

        return sshService.executeCommand(
            appServer,
            LogFileServiceCommands.COMMAND_TAIL,
            List.of(
                "-n " + lineCount,
                appServer.getFilePaths().logs()
                    + "/"
                    + serverLog.path()));
    }

    /**
     * Предоставляет указанный полный лог-файл указанного сервера, упакованный в ZIP.
     */
    public ResponseEntity<Resource> getEntireLogFile(
        final int serverId,
        final String logId
    ) {
        final AppServer appServer = appServerConfig.get(serverId);
        final LogFile serverLog = logFileConfig.get(logId);

        final String result = sshService.executeCommand(
            appServer,
            LogFileServiceCommands.COMMAND_ZIP,
            List.of(
                appServer.getFilePaths().logs() + "/" + serverLog.path(),
                "| base64"
            ));
            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set(
                HttpHeaders.CONTENT_DISPOSITION,
                serverId
                + "_" + serverLog.id()
                + "_" + logFileConfig.getDateFormatLogFile().format(LocalDateTime.now())
                + ".zip");
            final byte[] decoded = Base64.getDecoder().decode(
                StringUtils.replace(result, "\r\n", ""));
            return new ResponseEntity<>(
                new ByteArrayResource(decoded),
                responseHeaders,
                HttpStatus.OK);
    }

    /**
     * Предоставляет архив со всеми логами указанного сервера приложения.
     */
    public Mono<ResponseEntity<Resource>> getLogsArchive(
        final int serverId
    ) {
        final AppServer appServer = appServerConfig.get(serverId);

        return client.get()
            .uri("http://" + appServer.getHost() + "/log/archive")
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
