package carmarket.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(@NotBlank @Size(max = 50) @Pattern(regexp = "[^\\p{Cntrl}]+") String login,
                           String password) {
    @Override public String toString() { return "LoginRequest[redacted]"; }
}
