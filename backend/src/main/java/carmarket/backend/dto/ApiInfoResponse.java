package carmarket.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Информация об API автосалона")
public record ApiInfoResponse(
        @Schema(description = "Имя приложения", example = "carmarket-backend") String name,
        @Schema(description = "Версия контракта API", example = "v1") String apiVersion
) {
}
