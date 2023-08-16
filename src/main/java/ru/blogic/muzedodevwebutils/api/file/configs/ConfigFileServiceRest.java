package ru.blogic.muzedodevwebutils.api.file.configs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/file/config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileServiceRest {
    ConfigFileService configFileService;

    @GetMapping(path = "{serverId}", produces = "application/json")
    public ConfigFileService.GetConfigFileResponse get(
        @PathVariable final int serverId
    ) {
        return configFileService.getConfigFile(serverId);
    }
}
