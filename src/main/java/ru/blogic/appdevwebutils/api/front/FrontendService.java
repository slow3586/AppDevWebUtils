package ru.blogic.appdevwebutils.api.front;

import io.vavr.Predicates;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.api.command.config.CommandConfig;
import ru.blogic.appdevwebutils.api.file.configs.config.ConfigFileConfig;
import ru.blogic.appdevwebutils.api.file.logs.config.LogFileConfig;
import ru.blogic.appdevwebutils.api.front.dto.GetFrontendConfigResponse;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

/**
 * Сервис, предоставляющий конфигурацию frontend клиента пользователю.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class FrontendService {
    CommandConfig commandConfig;
    AppServerConfig appServerConfig;
    LogFileConfig logFileConfig;
    ConfigFileConfig configFileConfig;

    @NonFinal
    GetFrontendConfigResponse frontendConfigResponse;

    @NonFinal
    @Value("${app.version}")
    String actualVersion;

    /**
     * Формирование информации при запуске приложения.
     */
    @PostConstruct
    public void postConstruct() {
        this.frontendConfigResponse = new GetFrontendConfigResponse(
            this.actualVersion,
            commandConfig.getCommands()
                .filter(Predicates.not(Command::hidden))
                .map(c -> new GetFrontendConfigResponse.GetFrontendConfigResponseCommand(
                    c.id(),
                    c.name(),
                    c.blocksWsadmin()
                )),
            appServerConfig.getAll().map(AppServer::getId),
            configFileConfig.getConfigFiles().map(c ->
                new GetFrontendConfigResponse.GetFrontendConfigResponseConfig(
                    c.id()
                )),
            logFileConfig.getLogFiles().map(c ->
                new GetFrontendConfigResponse.GetFrontendConfigResponseLog(
                    c.id()
                ))
        );
    }

    /**
     * Предоставляет конфигурацию frontend клиента.
     */
    @DisableLoggingAspect
    public GetFrontendConfigResponse getFrontendConfig() {
        return this.frontendConfigResponse;
    }
}
