package ru.blogic.muzedodevwebutils.api.file.configs;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.file.configs.dao.ConfigFileDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.SSHService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileService {
    MuzedoServerDao muzedoServerDao;
    SSHService sshService;
    ConfigFileDao configFileDao;

    static Command command = new Command(
        "cat",
        "Cat",
        Command.Shell.SSH,
        false,
        true,
        "cat",
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );

    public Mono<GetConfigFileResponse> getServerConfigFile(
        final int serverId,
        final String configId
    ) {
        val muzedoServer = muzedoServerDao.get(serverId);
        val serverConfig = configFileDao.get(configId);

        val response = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            command,
            List.of(
                muzedoServer.getFilePaths().configsFilePath()
                    + serverConfig.getPath()));

        return response.map(r ->
            new GetConfigFileResponse(r.commandOutput()));
    }

    public record GetConfigFileRequest(
        int serverId,
        String configId
    ) {
    }

    public record GetConfigFileResponse(
        String text
    ) {
    }
}
