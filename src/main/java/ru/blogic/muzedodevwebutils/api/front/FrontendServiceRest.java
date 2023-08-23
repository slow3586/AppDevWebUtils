package ru.blogic.muzedodevwebutils.api.front;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FrontendServiceRest {
    FrontendService frontendService;

    @GetMapping(produces = "application/json")
    public FrontendService.GetFrontendConfigResponse getFrontendConfig() {
        return frontendService.getFrontendConfig();
    }
}
