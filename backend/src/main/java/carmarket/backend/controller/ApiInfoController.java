package carmarket.backend.controller;

import carmarket.backend.dto.ApiInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Система", description = "Общая информация об API автосалона")
public class ApiInfoController {

    private final String applicationName;

    public ApiInfoController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    @Operation(summary = "Получить имя приложения и версию API")
    public ApiInfoResponse info() {
        return new ApiInfoResponse(applicationName, "v1");
    }
}
