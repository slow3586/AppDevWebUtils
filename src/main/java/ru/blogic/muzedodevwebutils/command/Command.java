package ru.blogic.muzedodevwebutils.command;

import java.util.Optional;

public record Command(
    String id,
    String name,
    Optional<String> text,
    Shell shell,
    Effect effect,
    String command
) {
    public enum Shell {
        NONE,
        WSADMIN,
        SSH
    }

    public enum Effect {
        SERVER_BLOCK,
        WS_BLOCK,
        NONE
    }
}
