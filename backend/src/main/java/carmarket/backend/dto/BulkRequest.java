package carmarket.backend.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
public record BulkRequest(@NotNull Action action, @NotNull @Size(min=1,max=100) List<@NotNull @Valid Item> items) {
    public enum Action { ARCHIVE, RESTORE, DELETE }
    public record Item(@NotNull UUID id, @NotNull @PositiveOrZero Long version) { }
}
