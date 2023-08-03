package ru.blogic.muzedodevwebutils;

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

    @Autowired
    public CommandServiceRest(
        CommandService commandService
    ) {
        this.commandService = commandService;
    }

    @GetMapping(path = "run/{host}/{type}/{command}", produces = "application/json")
    public String runGet(
        @PathVariable("host") int host,
        @PathVariable("type") RunCommandRequest.Type type,
        @PathVariable("command") String command
    ) {
        return commandService.run(
            new RunCommandRequest(
                host,
                command,
                type));
    }

    @PostMapping(path = "run", produces = "application/json")
    public String runPost(
        @RequestBody final RunCommandRequest runCommandRequest
    ) {
        return commandService.run(
            runCommandRequest);
    }
}
