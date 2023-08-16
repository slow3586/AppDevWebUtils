package ru.blogic.muzedodevwebutils.api.command;

import java.util.List;

public record Command(
    String id,
    String name,
    Shell shell,
    boolean blocksWsadmin,
    String command,
    String readyPattern,
    int timeout,
    boolean announce,
    List<String> errPatterns
) {
    public enum Shell {
        NONE,
        WSADMIN,
        SSH
    }

    public enum Block {
        SERVER,
        WSADMIN,
        NONE
    }

    public static final String SSH_READY_PATTERN = "]#";
    public static final String WSADMIN_READY_PATTERN = "n>";
    public static final List<String> WSADMIN_ERR_PATTERNS
        = List.of("com.ibm.ws.scripting.ScriptingException",
        "Error creating \"SOAP\" connection",
        "syntax error",
        "SyntaxError",
        "root@edo-dev");
    public static final List<String> SSH_ERR_PATTERNS
        = List.of();
}
