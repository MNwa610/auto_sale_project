package carmarket.backend.dto;

import jakarta.validation.constraints.*;

public record CatalogWriteRequest(@NotBlank @Size(max = 100) @Pattern(regexp = "[^\\p{Cntrl}]+") String name,
                                   @Positive Long brandId, @NotNull Boolean enabled, @PositiveOrZero Long version) { }
