package ru.blogic.muzedodevwebutils.command;

import java.util.Optional;

public record Command(
    String id,
    String name,
    Optional<String> text,
    Type type,
    Effect effect,
    String command
) {
    public enum Type {
        NONE,
        WSADMIN,
        SSH
    }

    public enum Effect {
        SERVER_BLOCK,
        WS_CRIT,
        NONE
    }
}
