package ru.blogic.appdevwebutils.api.file.logs;

import ru.blogic.appdevwebutils.api.command.Command;

/**
 * Операции, использующиеся в LogFileService
 */
final class LogFileServiceCommands {
    final static Command COMMAND_TAIL = new Command(
        "tail",
        "Tail",
        Command.Shell.SSH,
        false,
        true,
        "tail",
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );
    
    final static Command COMMAND_ZIP = new Command(
        "zip",
        "Zip",
        Command.Shell.SSH,
        false,
        true,
        "zip -9 -j -q -", //-9 уровень сжатия, -j без папок, -q без лишней инфы
        Command.SSH_READY_PATTERN,
        10,
        false,
        Command.SSH_ERR_PATTERNS
    );
}
