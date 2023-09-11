package ru.blogic.muzedodevwebutils.api.file.configs;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.file.configs.dao.ConfigFileDao;
import ru.blogic.muzedodevwebutils.api.history.HistoryService;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServer;
import ru.blogic.muzedodevwebutils.api.muzedo.MuzedoServerDao;
import ru.blogic.muzedodevwebutils.api.muzedo.ssh.SSHService;
import ru.blogic.muzedodevwebutils.utils.Utils;

import java.util.Arrays;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileService {
    MuzedoServerDao muzedoServerDao;
    SSHService sshService;
    ConfigFileDao configFileDao;
    HistoryService historyService;

    static Command GET_CONFIG_COMMAND = new Command(
        "awk",
        "Awk",
        Command.Shell.SSH,
        false,
        true,
        "awk 1",
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );

    static Command SAVE_CONFIG_COMMAND = new Command(
        "echo",
        "Echo",
        Command.Shell.SSH,
        false,
        true,
        "echo",
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );

    public Mono<GetConfigFileResponse> getServerConfigFile(
        final int serverId,
        final String configId
    ) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(serverId);
        final ConfigFile serverConfig = configFileDao.get(configId);

        final Mono<SSHService.ExecuteCommandResult> response = sshService.executeCommand(
            muzedoServer.getSshClientSession(),
            GET_CONFIG_COMMAND,
            List.of(
                muzedoServer.getFilePaths().configsFilePath()
                    + "/"
                    + serverConfig.path()));

        return response
            .map(r ->
                new GetConfigFileResponse(r.commandOutput()));
    }

    public void saveServerConfigFile(
        final SaveConfigFileRequest saveConfigFileRequest
    ) {
        final MuzedoServer muzedoServer = muzedoServerDao.get(saveConfigFileRequest.serverId());
        try {
            final ConfigFile serverConfig = configFileDao.get(saveConfigFileRequest.configId());

            final String commentText = Option.of(saveConfigFileRequest.comment())
                .filter(StringUtils::isNotBlank)
                .map(s -> ": " + s)
                .getOrElse("");

            final String historyText;
            if (!saveConfigFileRequest.skipAnalysis()) {
                final String serverConfigFile = this.getServerConfigFile(
                        saveConfigFileRequest.serverId(),
                        saveConfigFileRequest.configId()
                    ).map(GetConfigFileResponse::text)
                    .block();

                if (StringUtils.isBlank(serverConfigFile)) {
                    throw new RuntimeException("При запросе конфиг со стенда пришел пустым или не пришел");
                }

                final List<String> previousVersion = Utils.splitByLines(serverConfigFile);
                final List<String> newVersion = Utils.splitByLines(saveConfigFileRequest.configText());

                final int sizeDifference = newVersion.size() - previousVersion.size();
                if (sizeDifference > 1 || sizeDifference < -1) {
                    throw new RuntimeException("Анализ: в конфиге было ДОБАВЛЕНО/УБРАНО более 1 строки: " +
                        "изменено " + sizeDifference + " строк");
                }

                final List<Tuple2<Tuple2<String, String>, Integer>> differentLines = previousVersion
                    .zip(newVersion)
                    .zipWithIndex()
                    .filter(t -> !StringUtils.equalsIgnoreCase(t._1._1, t._1._2));
                if (sizeDifference == 0 && differentLines.size() != 1) {
                    throw new RuntimeException("Анализ: в конфиге было ИЗМЕНЕНО более/менее 1 строки: "
                        + "изменено " + sizeDifference + " строк: "
                        + differentLines.map(l -> "№" + l._2).mkString(", "));
                }
                final Tuple2<Tuple2<String, String>, Integer> changedLine = differentLines.head();

                historyText = "Изменена строка конфига " +
                    "\"" + serverConfig.id() + "\" " +
                    "#" + changedLine._2() + ": "
                    + "\"" + changedLine._1()._2 + "\""
                    + commentText;
            } else {
                historyText = "Изменен конфиг " +
                    "\"" + serverConfig.id() + "\" "
                    + "без анализа"
                    + commentText;
            }

            final SSHService.ExecuteCommandResult saveResult = sshService.executeCommand(
                muzedoServer.getSshClientSession(),
                SAVE_CONFIG_COMMAND,
                List.of(
                    "'" + saveConfigFileRequest.configText()
                        + "' >| "
                        + muzedoServer.getFilePaths().configsFilePath()
                        + "/"
                        + serverConfig.path())
            ).block();

            historyService.addHistoryEntry(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.CRIT,
                historyText
            );
        } catch (Exception e) {
            historyService.addHistoryEntry(
                muzedoServer.getId(),
                MuzedoServer.HistoryEntry.Severity.INFO,
                "Ошибка изменения конфига: " + e.getMessage());
            throw new RuntimeException("#saveServerConfigFile", e);
        }
    }

    public record GetConfigFileRequest(
        int serverId,
        String configId
    ) {}

    public record GetConfigFileResponse(
        String text
    ) {}

    public record SaveConfigFileRequest(
        int serverId,
        String configId,
        String configText,
        String comment,
        boolean skipAnalysis
    ) {}
}
