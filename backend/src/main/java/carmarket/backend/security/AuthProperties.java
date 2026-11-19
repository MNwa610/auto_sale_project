package carmarket.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties("carmarket.auth")
public record AuthProperties(String secretBase64,
                             @DefaultValue("carmarket") String issuer,
                             @DefaultValue("PT10M") Duration accessTtl,
                             @DefaultValue("P7D") Duration refreshTtl,
                             @DefaultValue("5") int maxFailedAttempts,
                             @DefaultValue("PT15M") Duration lockDuration,
                             @DefaultValue("PT15M") Duration failureWindow) {
    public AuthProperties {
        if (issuer == null || issuer.isBlank() || accessTtl == null || refreshTtl == null
                || accessTtl.compareTo(Duration.ofMinutes(1)) < 0 || accessTtl.compareTo(Duration.ofMinutes(15)) > 0
                || refreshTtl.compareTo(Duration.ofHours(1)) < 0 || refreshTtl.compareTo(Duration.ofDays(30)) > 0
                || maxFailedAttempts < 1 || maxFailedAttempts > 20
                || lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()
                || failureWindow == null || failureWindow.isNegative() || failureWindow.isZero()) {
            throw new IllegalArgumentException("Некорректные сроки или лимиты carmarket.auth");
        }
    }

    @Override public String toString() { return "AuthProperties[secret redacted]"; }
}
