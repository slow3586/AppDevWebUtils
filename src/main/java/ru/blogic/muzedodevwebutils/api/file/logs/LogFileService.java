package ru.blogic.muzedodevwebutils.api.file.logs;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.file.logs.dao.LogFileDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.ssh.SSHService;
import ru.blogic.muzedodevwebutils.utils.Utils;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogFileService {
    MuzedoServerDao muzedoServerDao;
    SSHService sshService;
    LogFileDao logFileDao;

    // ПУТЬ К ФАЙЛУ НЕ ДОЛЖЕН БЫТЬ ДЛИННЫМ, ИНАЧЕ SSH БУДЕТ ВСТАВЛЯТЬ ЛЕВЫЕ СИМВОЛЫ
    // В ТАКОМ СЛУЧАЕ НУЖНО ИСПОЛЬЗОВАТЬ SYMLINKИ
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
        final MuzedoServer muzedoServer = muzedoServerDao.get(request.serverId());
        final LogFile serverLog = logFileDao.get(request.logId());

        final int lineCount = Utils.clamp(request.linesCount(), 1, 1000);

        final Mono<SSHService.ExecuteCommandResult> response = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            command,
            List.of(
                "-n " + lineCount,
                muzedoServer.getFilePaths().logsFilePath()
                    + "/"
                    + serverLog.path()));

        return response.map(r ->
            new GetLogFileResponse(r.commandOutput().mkString("\n")));
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
