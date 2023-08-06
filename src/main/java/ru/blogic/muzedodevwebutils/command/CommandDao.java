package ru.blogic.muzedodevwebutils.command;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@DisableLoggingAspect
public class CommandDao {
    private final List<Command> commands = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        commands.add(new Command("announce",
            "Оповещение",
            Optional.of(""),
            Command.Shell.NONE,
            Command.Block.NONE,
            "",
            "",
            10));
        commands.add(new Command("hostname",
            "Проверка",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.NONE,
            "hostname",
            Command.SSH_READY,
            10));
        commands.add(new Command("ra",
            "Рестарт",
            Optional.empty(),
            Command.Shell.WSADMIN,
            Command.Block.WSADMIN,
            "ra(1)",
            Command.WSADMIN_READY,
            180));
        commands.add(new Command("ura",
            "Обновление",
            Optional.empty(),
            Command.Shell.WSADMIN,
            Command.Block.WSADMIN,
            "ura(1)",
            Command.WSADMIN_READY,
            480));
        commands.add(new Command("clear_cache",
            "Клир кэш",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.SERVER,
            "/root/deploy/clear_cache_shortcut.sh",
            Command.SSH_READY,
            900));
        commands.add(new Command("cd_root_deploy",
            "cd_root_deploy",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.WSADMIN,
            "cd /root/deploy/",
            Command.SSH_READY,
            10));
        commands.add(new Command("wsadmin_start",
            "wsadmin_start",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.WSADMIN,
            "./wsadmin_extra.sh",
            Command.WSADMIN_READY,
            60));
    }

    public Command get(String id) {
        return commands
            .stream()
            .filter(c -> c.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "Не найдена команда " + id));
    }
}
