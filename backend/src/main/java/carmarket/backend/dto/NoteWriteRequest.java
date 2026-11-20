package carmarket.backend.dto;
import jakarta.validation.constraints.*;
public record NoteWriteRequest(@NotBlank @Size(max=4000) String content, @NotNull Boolean pinned, @PositiveOrZero Long version) { }
