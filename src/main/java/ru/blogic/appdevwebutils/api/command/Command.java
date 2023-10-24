package ru.blogic.appdevwebutils.api.command;

import io.vavr.collection.List;

/**
 * Сущность, хранящая информацию об операции, которую можно выполнить на сервере.
 * @param id ID операции
 * @param name Название операции для интерфейса пользователя
 * @param shell Оболочка, в которой выполняется операция
 * @param blocksWsadmin Блокирует ли операция WsAdmin
 * @param hidden Спрятана ли операция от пользователя
 * @param command Команда, выполняемая в оболочке
 * @param readyPattern Шаблоны текста, сигнализирующие об окончании выполнения операции в оболочке
 * @param timeout Время, через которое операция считается неуспешной
 * @param announce Требуется ли оповещение о старте операции
 * @param errPatterns Шаблоны текста, сигнализирующие об ошибке выполнения операции в оболочке
 */
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
        ": cannot stat ",
        "Permission denied");
    public static final List<String> SSH_ERR_PATTERNS
        = List.of(": cannot open ",
        ": No such file or directory",
        ": cannot stat ");
}
