package carmarket.backend.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
    @Override public String toString() { return "ChangePasswordRequest[redacted]"; }
}
