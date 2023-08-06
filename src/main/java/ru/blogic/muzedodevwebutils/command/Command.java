package ru.blogic.muzedodevwebutils.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record Command(
    String id,
    String name,
    Optional<String> text,
    Shell shell,
    Block blocks,
    String command,
    String readySymbol,
    int timeout,
    boolean announce,
    List<String> errTexts
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

    public static final String SSH_READY = "#";
    public static final String WSADMIN_READY = ">";
    public static final List<String> WSADMIN_ERRTEXTS
        = List.of("com.ibm.ws.scripting.ScriptingException",
        "Error creating \"SOAP\" connection");
}
