package ru.blogic.muzedodevwebutils.api.file.configs;

import io.vavr.collection.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerService;
import ru.blogic.muzedodevwebutils.api.command.CommandService;
import ru.blogic.muzedodevwebutils.api.muzedo.SSHService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileService {
    CommandService commandService;
    MuzedoServerService muzedoServerService;
    MuzedoServerDao muzedoServerDao;
    SSHService sshService;

    public GetConfigFileResponse getConfigFile(int serverId) {
        val muzedoServer = muzedoServerDao.get(serverId);

        val tail = new Command(
            "tail_100",
            "Tail",
            Command.Shell.SSH,
            false,
            true,
            "tail 100 /workdir/logs/UZDO-integration.log",
            Command.SSH_READY_PATTERN,
            10,
            false,
            Command.SSH_ERR_PATTERNS
        );

        val response = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            tail,
            List.empty(),
            null);

        return new GetConfigFileResponse();
    }

    public static class GetConfigFileResponse {
    }
}
