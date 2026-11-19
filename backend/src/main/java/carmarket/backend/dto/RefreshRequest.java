package carmarket.backend.dto;

public record RefreshRequest(String refreshToken) {
    @Override public String toString() { return "RefreshRequest[redacted]"; }
}
