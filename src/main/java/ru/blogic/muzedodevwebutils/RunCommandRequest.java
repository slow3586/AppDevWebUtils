package ru.blogic.muzedodevwebutils;

public record RunCommandRequest(
    int serverId,
    String command,
    Type type
) {
    public enum Type {
        WSADMIN,
        SSH
    }
}
