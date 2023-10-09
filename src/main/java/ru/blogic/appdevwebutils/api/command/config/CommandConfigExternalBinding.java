package ru.blogic.appdevwebutils.api.command.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.blogic.appdevwebutils.api.command.Command;

@ConfigurationProperties(prefix = "app.command")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class CommandConfigExternalBinding {
    java.util.List<CommandConfigExternalBindingDto> commands;

    @Data
    static class CommandConfigExternalBindingDto {
        String id;
        String name;
        String command;
        int timeout;
        Command.Shell shell;
        java.util.List<CommandConfigDtoFlags> flags;

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
