package ru.blogic.muzedodevwebutils.api.command.dao;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.api.command.Command;
import ru.blogic.muzedodevwebutils.api.command.dao.CommandConfig.CommandConfigDto.CommandConfigDtoFlags;
import ru.blogic.muzedodevwebutils.config.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@DisableLoggingAspect
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommandDao {
    CommandConfig commandConfig;
    List<Command> commands = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        commands.addAll(commandConfig.getCommands()
            .stream()
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
                        : Collections.emptyList()
            )).toList()
        );

        commands.add(new Command("cd_root_deploy",
            "cd_root_deploy",
            Command.Shell.SSH,
            true,
            true,
            "cd /root/deploy/",
            Command.SSH_READY_PATTERN,
            10,
            true,
            Collections.emptyList()));
        commands.add(new Command("wsadmin_start",
            "wsadmin_start",
            Command.Shell.SSH,
            true,
            true,
            "./wsadmin_extra.sh",
            Command.WSADMIN_READY_PATTERN,
            60,
            true,
            Command.WSADMIN_ERR_PATTERNS));
    }

    public Command get(String id) {
        return commands
            .stream()
            .filter(c -> c.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "Не найдена команда " + id));
    }

    public io.vavr.collection.List<Command> getAll(){
        return io.vavr.collection.List.ofAll(commands);
    }
}
