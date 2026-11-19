package carmarket.backend.dto;

import carmarket.backend.domain.CarStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

/** POST: version отсутствует. PUT: обязателен version из последнего ответа. */
public record CarWriteRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[^\\p{Cntrl}]+") String externalCode,
        @NotNull @Positive Long sellerId, @NotNull @Positive Long modelId,
        @NotNull @Min(1950) @Max(2100) Integer year, @NotNull @PositiveOrZero Integer mileage,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 40) String vin,
        @NotNull @Positive Long bodyTypeId, @NotNull @Positive Long transmissionId,
        @NotNull @Positive Long fuelTypeId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("99.9")
        @Digits(integer = 2, fraction = 1) BigDecimal engineVolume,
        @Size(max = 4000) String description, @NotNull CarStatus status, @NotNull Boolean certified,
        @NotNull @Size(max = 20) Set<@NotBlank @Size(max = 50) @Pattern(regexp = "[^\\p{Cntrl}]+") String> tags,
        @PositiveOrZero Long version) { }
