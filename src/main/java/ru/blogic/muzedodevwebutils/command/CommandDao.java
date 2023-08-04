package ru.blogic.muzedodevwebutils.command;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CommandDao {
    private final List<Command> commands = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        commands.add(new Command("announce",
            "Оповещение",
            Optional.of(""),
            Command.Type.NONE,
            Command.Effect.NONE,
            ""));
        commands.add(new Command("hostname",
            "Проверка",
            Optional.empty(),
            Command.Type.SSH,
            Command.Effect.NONE,
            "hostname"));
        commands.add(new Command("ra",
            "Рестарт",
            Optional.empty(),
            Command.Type.WSADMIN,
            Command.Effect.WS_CRIT,
            "ra(1)"));
        commands.add(new Command("ura",
            "Обновление",
            Optional.empty(),
            Command.Type.WSADMIN,
            Command.Effect.WS_CRIT,
            "ura(1)"));
        commands.add(new Command("clear_cache",
            "Клир кэш",
            Optional.empty(),
            Command.Type.SSH,
            Command.Effect.WS_CRIT,
            "/root/deploy/clear_cache_shortcut.sh"));
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
