package ru.blogic.appdevwebutils.api.front;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.blogic.appdevwebutils.api.front.dto.GetFrontendConfigResponse;

/**
 * REST сервис, предоставляющий конфигурацию frontend-а клиенту пользователя.
 */
@RestController
@RequestMapping("api/front")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class FrontendServiceRest {
    FrontendService frontendService;

    @GetMapping(produces = "application/json")
    public GetFrontendConfigResponse getFrontendConfig() {
        return frontendService.getFrontendConfig();
    }
}
