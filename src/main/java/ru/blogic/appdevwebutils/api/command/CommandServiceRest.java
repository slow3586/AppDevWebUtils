package ru.blogic.appdevwebutils.api.command;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.appdevwebutils.api.command.dto.CommandCancelRequest;
import ru.blogic.appdevwebutils.api.command.dto.CommandDelayRequest;
import ru.blogic.appdevwebutils.api.command.dto.CommandRunRequest;

/**
 * REST сервис, отвечающий за выполнение и планирование операций пользователями на серверах приложений.
 */
@RestController
@RequestMapping("api/command")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommandServiceRest {
    CommandService commandService;

    @PostMapping(path = "run", produces = "application/json")
    public void runPost(
        @RequestBody final CommandRunRequest commandRunRequest
    ) {
        commandService.run(commandRunRequest);
    }

    @PostMapping(path = "delay", produces = "application/json")
    public void delay(
        @RequestBody final CommandDelayRequest commandDelayRequest
    ) {
        commandService.delay(commandDelayRequest);
    }

    @PostMapping(path = "cancel", produces = "application/json")
    public void cancel(
        @RequestBody final CommandCancelRequest commandCancelRequest
    ) {
        commandService.cancel(commandCancelRequest);
    }
}
