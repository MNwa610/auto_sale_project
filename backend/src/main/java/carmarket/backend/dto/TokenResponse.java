package carmarket.backend.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                            long expiresInSeconds, Instant refreshExpiresAt, CurrentUserResponse user) {
    @Override public String toString() { return "TokenResponse[redacted]"; }
}
