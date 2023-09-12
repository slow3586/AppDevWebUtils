package ru.blogic.muzedodevwebutils.api.front;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.muzedodevwebutils.api.front.dto.GetFrontendConfigResponse;

@RestController
@RequestMapping("api/front")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FrontendServiceRest {
    FrontendService frontendService;

    @GetMapping(produces = "application/json")
    public GetFrontendConfigResponse getFrontendConfig() {
        return frontendService.getFrontendConfig();
    }
}
