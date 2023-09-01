package ru.blogic.muzedodevwebutils.api.file.configs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/file/config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileServiceRest {
    ConfigFileService configFileService;

    @GetMapping(path = "{serverId}/{configId}", produces = "application/json")
    public Mono<ConfigFileService.GetConfigFileResponse> getServerConfigFile(
        @PathVariable final int serverId,
        @PathVariable final String configId
    ) {
        return configFileService.getServerConfigFile(serverId, configId);
    }
}
