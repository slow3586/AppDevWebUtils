package ru.blogic.appdevwebutils.api.file.configs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.appdevwebutils.api.file.configs.dto.SaveConfigFileRequest;

/**
 * REST сервис, отвечающий за показ и изменения конфигов сервера приложения.
 */
@RestController
@RequestMapping("api/file/config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class ConfigFileServiceRest {
    ConfigFileService configFileService;

    @GetMapping(path = "{serverId}/{configId}", produces = "application/json")
    public String getServerConfigFile(
        @PathVariable final int serverId,
        @PathVariable final String configId
    ) {
        return configFileService.getServerConfigFile(serverId, configId);
    }

    @PostMapping(path = "save")
    public void saveServerConfigFile(
        @RequestBody final SaveConfigFileRequest saveConfigFileRequest
    ) {
        configFileService.saveServerConfigFile(saveConfigFileRequest);
    }
}
