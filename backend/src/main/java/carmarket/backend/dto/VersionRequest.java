package carmarket.backend.dto;
import jakarta.validation.constraints.*;
public record VersionRequest(@NotNull @PositiveOrZero Long version) { }
