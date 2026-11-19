package carmarket.backend.dto;

import jakarta.validation.constraints.*;

public record CatalogEnabledRequest(@NotNull Boolean enabled, @NotNull @PositiveOrZero Long version) { }
