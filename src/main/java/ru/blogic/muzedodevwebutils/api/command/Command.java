package ru.blogic.muzedodevwebutils.api.command;

import io.vavr.collection.List;

public record Command(
    String id,
    String name,
    Shell shell,
    boolean blocksWsadmin,
    boolean hidden,
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

    public static final String SSH_READY_PATTERN = "]#";
    public static final String WSADMIN_READY_PATTERN = "n>";
    public static final List<String> WSADMIN_ERR_PATTERNS
        = List.of("com.ibm.ws.scripting.ScriptingException",
        "Error creating \"SOAP\" connection",
        "syntax error",
        "SyntaxError",
        "root@edo-dev",
        ": No such file or directory",
        ": cannot stat ");
    public static final List<String> SSH_ERR_PATTERNS
        = List.of(": cannot open ",
        ": No such file or directory",
        ": cannot stat ");
}
