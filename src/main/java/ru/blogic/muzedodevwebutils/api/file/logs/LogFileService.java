package ru.blogic.muzedodevwebutils.api.file.logs;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.file.logs.dao.LogFileDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.SSHService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileService {
    MuzedoServerDao muzedoServerDao;
    SSHService sshService;
    LogFileDao logFileDao;

    static Command command = new Command(
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

    public Mono<GetLogFileResponse> getServerLogFile(
        final GetLogFileRequest request
    ) {
        val muzedoServer = muzedoServerDao.get(request.serverId());
        val serverLog = logFileDao.get(request.logId());

        val response = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            command,
            List.of(
                "-n " + request.linesCount(),
                muzedoServer.getFilePaths().logsFilePath()
                    + serverLog.getPath()));

        return response.map(r ->
            new GetLogFileResponse(r.commandOutput()));
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
