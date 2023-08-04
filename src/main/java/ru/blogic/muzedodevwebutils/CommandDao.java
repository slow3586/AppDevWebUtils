package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CommandDao {
    private final List<Command> commands = new ArrayList<>();

    @PostConstruct
    public void postConstruct(){
        commands.add(new Command("hostname", "Проверка",
            Command.Type.SSH,
            Command.Effect.NONE,
            "hostname"));
        commands.add(new Command("ra", "Рестарт",
            Command.Type.WSADMIN,
            Command.Effect.WS_CRIT,
            "ra(1)"));
        commands.add(new Command("ura", "Обновление",
            Command.Type.WSADMIN,
            Command.Effect.WS_CRIT,
            "ura(1)"));
        commands.add(new Command("clear_cache", "Клир кэш",
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
