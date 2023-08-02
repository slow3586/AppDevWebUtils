package ru.blogic.muzedodevwebutils;

import lombok.Value;

public record RunCommandRequest(
    String host,
    String command,
    Type type
) {
    public enum Type {
        WSADMIN,
        SSH
    }
}
