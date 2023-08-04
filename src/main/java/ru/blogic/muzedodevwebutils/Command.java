package ru.blogic.muzedodevwebutils;

public record Command(
    String id,
    String name,
    Type type,
    Effect effect,
    String command
) {
    public enum Type {
        WSADMIN,
        SSH
    }

    public enum Effect {
        SERVER_BLOCK,
        WS_CRIT,
        NONE
    }
}
