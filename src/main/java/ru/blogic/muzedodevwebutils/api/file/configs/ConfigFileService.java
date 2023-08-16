package ru.blogic.muzedodevwebutils.api.file.configs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerService;
import ru.blogic.muzedodevwebutils.api.command.CommandService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileService {
    CommandService commandService;
    MuzedoServerService muzedoServerService;

    public GetConfigFileResponse getConfigFile(int serverId) {
        return null;
    }

    public static class GetConfigFileResponse {
    }
}
