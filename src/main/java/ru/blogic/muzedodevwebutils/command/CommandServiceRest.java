package ru.blogic.muzedodevwebutils.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/command")
public class CommandServiceRest {
    private final CommandService commandService;

    public CommandServiceRest(
        CommandService commandService
    ) {
        this.commandService = commandService;
    }

    @GetMapping(path = "run/{host}/{command}", produces = "application/json")
    public String runGet(
        @PathVariable("host") int serverId,
        @PathVariable("command") String commandId
    ) {
        return commandService.run(
            new CommandRunRequest(
                serverId,
                commandId,
                "",
                0));
    }

    @PostMapping(path = "run", produces = "application/json")
    public String runPost(
        @RequestBody final CommandRunRequest commandRunRequest
    ) {
        return commandService.run(
            commandRunRequest);
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
