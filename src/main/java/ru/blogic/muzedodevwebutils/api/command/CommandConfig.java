package ru.blogic.muzedodevwebutils.api.command;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.command")
@RequiredArgsConstructor
@Getter
public class CommandConfig {
    final List<CommandConfigDto> commands;

    @Data
    public static class CommandConfigDto {
        String id;
        String name;
        String command;
        int timeout;
        Command.Shell shell;
        List<CommandConfigDtoFlags> flags;

        public enum CommandConfigDtoFlags {
            ANNOUNCE_EXECUTION,
            WSADMIN_BLOCK,
            WSADMIN_RESTART_ON_END,
            WSADMIN_ERR_PATTERNS,
            WSADMIN_READY_PATTERN,
            SSH_BLOCK,
            SSH_RESTART_ON_END,
            SSH_ERR_PATTERNS,
            SSH_READY_PATTERN
        }
    }
}
