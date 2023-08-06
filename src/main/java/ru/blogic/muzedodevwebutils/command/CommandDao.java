package ru.blogic.muzedodevwebutils.command;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@DisableLoggingAspect
public class CommandDao {
    private final List<Command> commands = new ArrayList<>();

    @PostConstruct
    public void postConstruct() {
        // com.ibm.ws.scripting.ScriptingException:
        commands.add(new Command("announce",
            "Оповещение",
            Optional.of(""),
            Command.Shell.NONE,
            Command.Block.NONE,
            "",
            "",
            10,
            true,
            Collections.emptyList()));
        commands.add(new Command("hostname",
            "Проверка",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.NONE,
            "hostname",
            Command.SSH_READY,
            10,
            false,
            Collections.emptyList()));
        commands.add(new Command("ra",
            "Рестарт",
            Optional.empty(),
            Command.Shell.WSADMIN,
            Command.Block.WSADMIN,
            "ra(1)",
            Command.WSADMIN_READY,
            180,
            true,
            Command.WSADMIN_ERRTEXTS));
        commands.add(new Command("ura",
            "Обновление (gp + integ + cfg)",
            Optional.empty(),
            Command.Shell.WSADMIN,
            Command.Block.WSADMIN,
            "ura(1)",
            Command.WSADMIN_READY,
            480,
            true,
            Command.WSADMIN_ERRTEXTS));
        commands.add(new Command("uric",
            "Обновление (integ + cfg)",
            Optional.empty(),
            Command.Shell.WSADMIN,
            Command.Block.WSADMIN,
            "uric()",
            Command.WSADMIN_READY,
            480,
            true,
            Command.WSADMIN_ERRTEXTS));
        commands.add(new Command("clear_cache",
            "Клир кэш",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.SERVER,
            "/root/deploy/clear_cache_shortcut.sh",
            Command.SSH_READY,
            900,
            true,
            Collections.emptyList()));
        commands.add(new Command("cd_root_deploy",
            "cd_root_deploy",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.WSADMIN,
            "cd /root/deploy/",
            Command.SSH_READY,
            10,
            true,
            Collections.emptyList()));
        commands.add(new Command("wsadmin_start",
            "wsadmin_start",
            Optional.empty(),
            Command.Shell.SSH,
            Command.Block.WSADMIN,
            "./wsadmin_extra.sh",
            Command.WSADMIN_READY,
            60,
            true,
            Command.WSADMIN_ERRTEXTS));
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
