package ru.blogic.appdevwebutils.api.command.config;

import io.vavr.collection.List;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ru.blogic.appdevwebutils.api.command.Command;
import ru.blogic.appdevwebutils.api.command.config.CommandConfigExternalBinding.CommandConfigExternalBindingDto.CommandConfigDtoFlags;
import ru.blogic.appdevwebutils.config.logging.DisableLoggingAspect;

/** Конфигурация сервиса операций. */
@Component
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class CommandConfig {
    /** Внешняя конфигурация. */
    final CommandConfigExternalBinding commandConfigExternalBinding;

    List<Command> commands;

    /** Конвертация внешней конфигурации. */
    @PostConstruct
    public void postConstruct() {
        this.commands = List.ofAll(commandConfigExternalBinding.getCommands())
            .map(c -> new Command(
                c.getId(),
                c.getName(),
                c.getShell(),
                c.getFlags().contains(CommandConfigDtoFlags.WSADMIN_BLOCK),
                false,
                c.getCommand(),
                c.getFlags().contains(CommandConfigDtoFlags.SSH_READY_PATTERN)
                    ? Command.SSH_READY_PATTERN
                    : c.getFlags().contains(CommandConfigDtoFlags.WSADMIN_READY_PATTERN)
                        ? Command.WSADMIN_READY_PATTERN
                        : null,
                c.getTimeout(),
                c.flags.contains(CommandConfigDtoFlags.ANNOUNCE_EXECUTION),
                c.getFlags().contains(CommandConfigDtoFlags.SSH_ERR_PATTERNS)
                    ? Command.SSH_ERR_PATTERNS
                    : c.getFlags().contains(CommandConfigDtoFlags.WSADMIN_ERR_PATTERNS)
                        ? Command.WSADMIN_ERR_PATTERNS
                        : List.empty()
            ));
    }

    /** Предоставляет информацию об операции с указанным ID. */
    public Command get(String id) {
        return commands
            .find(c -> c.id().equals(id))
            .getOrElseThrow(() -> new RuntimeException(
                "Не найдена команда " + id));
    }
}
