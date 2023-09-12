package ru.blogic.muzedodevwebutils.api.front;

import io.vavr.Predicates;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.command.config.CommandConfig;
import ru.blogic.muzedodevwebutils.api.file.configs.config.ConfigFileConfig;
import ru.blogic.muzedodevwebutils.api.file.logs.config.LogFileConfig;
import ru.blogic.muzedodevwebutils.api.front.dto.GetFrontendConfigResponse;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.config.MuzedoServerConfig;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FrontendService {
    CommandConfig commandConfig;
    MuzedoServerConfig muzedoServerConfig;
    LogFileConfig logFileConfig;
    ConfigFileConfig configFileConfig;

    @NonFinal
    GetFrontendConfigResponse frontendConfigResponse;

    @NonFinal
    @Value("${app.version}")
    String actualVersion;

    @PostConstruct
    public void postConstruct() {
        this.frontendConfigResponse = new GetFrontendConfigResponse(
            this.actualVersion,
            commandConfig.getAll()
                .filter(Predicates.not(Command::hidden))
                .map(c -> new GetFrontendConfigResponse.GetFrontendConfigResponseCommand(
                    c.id(),
                    c.name(),
                    c.blocksWsadmin()
                )),
            muzedoServerConfig.getAll().map(MuzedoServer::getId),
            configFileConfig.getAll().map(c ->
                new GetFrontendConfigResponse.GetFrontendConfigResponseConfig(
                    c.id()
                )),
            logFileConfig.getAll().map(c ->
                new GetFrontendConfigResponse.GetFrontendConfigResponseLog(
                    c.id()
                ))
        );
    }

    @DisableLoggingAspect
    public GetFrontendConfigResponse getFrontendConfig() {
        return this.frontendConfigResponse;
    }
}
