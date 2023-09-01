package ru.blogic.muzedodevwebutils.api.command;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandCancelRequest;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandDelayRequest;
import ru.blogic.muzedodevwebutils.api.command.dto.CommandRunRequest;

@RestController
@RequestMapping("api/command")
@RequiredArgsConstructor
public class CommandServiceRest {
    private final CommandService commandService;

    @GetMapping(path = "run/{host}/{command}", produces = "application/json")
    public void runGet(
        @PathVariable("host") int serverId,
        @PathVariable("command") String commandId
    ) {
        commandService.run(
            new CommandRunRequest(
                serverId,
                commandId,
                "",
                0));
    }

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
