package ru.blogic.appdevwebutils.api.file.configs;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.blogic.appdevwebutils.api.file.configs.config.ConfigFile;
import ru.blogic.appdevwebutils.api.file.configs.config.ConfigFileConfig;
import ru.blogic.appdevwebutils.api.file.configs.dto.SaveConfigFileRequest;
import ru.blogic.appdevwebutils.api.history.HistoryService;
import ru.blogic.appdevwebutils.api.app.AppServer;
import ru.blogic.appdevwebutils.api.app.config.AppServerConfig;
import ru.blogic.appdevwebutils.api.app.ssh.SshService;
import ru.blogic.appdevwebutils.utils.Utils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFileService {
    AppServerConfig appServerConfig;
    SshService sshService;
    ConfigFileConfig configFileConfig;
    HistoryService historyService;

    public String getServerConfigFile(
        final int serverId,
        final String configId
    ) {
        final AppServer appServer = appServerConfig.get(serverId);
        final ConfigFile configFile = configFileConfig.get(configId);

        final byte[] file = sshService.downloadFile(
            appServer,
            appServer.getFilePaths().configsFilePath()
                + "/"
                + configFile.path());

        return new String(
            file,
            StandardCharsets.UTF_8);
    }

    public void saveServerConfigFile(
        final SaveConfigFileRequest saveConfigFileRequest
    ) {
        try {
            final AppServer appServer = appServerConfig.get(saveConfigFileRequest.serverId());
            final ConfigFile configFile = configFileConfig.get(saveConfigFileRequest.configId());

            final Option<String> commentText = Option.of(saveConfigFileRequest.comment())
                .filter(StringUtils::isNotBlank)
                .map(s -> ": \"" + s + "\"");

            final String historyText;
            if (!saveConfigFileRequest.skipAnalysis()) {
                final String serverConfigFile = this.getServerConfigFile(
                    saveConfigFileRequest.serverId(),
                    saveConfigFileRequest.configId()
                );

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
                        + "изменено " + differentLines.size() + " строк"
                        + (!differentLines.isEmpty()
                        ? ": " + differentLines.map(l -> "№" + l._2).mkString(", ")
                        : ""));
                }
                if (sizeDifference != 0 && differentLines.size() != 1) {
                    throw new RuntimeException("Анализ: в конфиге были одновременно ИЗМЕНЕНЫ и ДОБАВЛЕНЫ/УБРАНЫ строки");
                }

                final Tuple2<Tuple2<String, String>, Integer> changedLine = differentLines.head();
                historyText = "Изменена строка конфига " +
                    "\"" + configFile.id() + "\" " +
                    "#" + changedLine._2() + ": "
                    + "\"" + StringUtils.substring(changedLine._1()._2, 0, 200) + "\""
                    + commentText.getOrElse("");
            } else {
                historyText = "Изменен конфиг " +
                    "\"" + configFile.id() + "\" "
                    + commentText.getOrElse("без комментария");
            }

            sshService.uploadFile(
                appServer,
                saveConfigFileRequest.configText().getBytes(),
                appServer.getFilePaths().configsFilePath()
                    + "/"
                    + configFile.path());

            historyService.addHistoryEntry(
                appServer.getId(),
                AppServer.HistoryEntry.Severity.CRIT,
                historyText);
        } catch (Exception e) {
            throw new RuntimeException("#saveServerConfigFile: " + e.getMessage(), e);
        }
    }

}
