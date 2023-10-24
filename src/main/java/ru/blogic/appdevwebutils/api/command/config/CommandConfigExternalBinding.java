package ru.blogic.appdevwebutils.api.command.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.blogic.appdevwebutils.api.command.Command;

/**
 * Внешняя конфигурация сервиса операций
 */
@ConfigurationProperties(prefix = "app.command")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class CommandConfigExternalBinding {
    java.util.List<CommandConfigExternalBindingDto> commands;

    /**
     * Конфигурация операции.
     */
    @Data
    static class CommandConfigExternalBindingDto {
        /**
         * ID операции
         */
        String id;
        /**
         * Название операции
         */
        String name;
        /**
         * Выполняемая команда
         */
        String command;
        /**
         * Время, через которое операция считается проваленной
         */
        int timeout;
        /**
         * Оболочка, в которой выполняется операция
         */
        Command.Shell shell;
        /**
         * Флаги выполнения операции
         */
        java.util.List<CommandConfigDtoFlags> flags;

        /**
         * Флаги выполнения операции
         */
        public enum CommandConfigDtoFlags {
            /**
             * Предупредить о планировании операции.
             */
            ANNOUNCE_EXECUTION,
            /**
             * Блокирует WsAdmin.
             */
            WSADMIN_BLOCK,
            /**
             * Требует перезапуска WsAdmin после выполнения.
             */
            WSADMIN_RESTART_ON_END,
            /**
             * Использует шаблоны ошибок оболочки WsAdmin.
             */
            WSADMIN_ERR_PATTERNS,
            /**
             * Использует шаблоны готовности оболочки WsAdmin.
             */
            WSADMIN_READY_PATTERN,
            /**
             * Использует шаблоны ошибок оболочки WsAdmin.
             */
            SSH_ERR_PATTERNS,
            /**
             * Использует шаблоны готовности оболочки WsAdmin.
             */
            SSH_READY_PATTERN
        }
    }
}
