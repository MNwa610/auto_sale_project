package carmarket.backend.security;

import carmarket.backend.dto.CurrentUserResponse;
import carmarket.backend.dto.TokenResponse;
import carmarket.backend.error.AuthFailure;
import org.springframework.http.HttpStatus;
import carmarket.backend.repository.AuthRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final AuthRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, AuthProperties properties, AuthRepository repository, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.repository = repository;
        this.clock = clock;
    }

    /** Вызывается только внутри транзакции AuthService после блокировки пользователя. */
    public TokenResponse issue(AuthRepository.Credential credential, UUID familyId, Instant familyExpiresAt) {
        Instant now = clock.instant();
        Instant expiresAt = familyExpiresAt == null ? now.plus(properties.refreshTtl()) : familyExpiresAt;
        if (!expiresAt.isAfter(now)) {
            throw new AuthFailure(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Сессия истекла. Войдите снова.");
        }
        Instant accessExpiresAt = now.plus(properties.accessTtl());
        if (accessExpiresAt.isAfter(expiresAt)) accessExpiresAt = expiresAt;
        UUID sessionId = UUID.randomUUID();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.createSession(sessionId, credential.userId(), familyId == null ? sessionId : familyId,
                hash(refresh), now, expiresAt);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of("carmarket-api"))
                .subject(Long.toString(credential.userId())).id(UUID.randomUUID().toString())
                .issuedAt(now).expiresAt(accessExpiresAt)
                .claim("sid", sessionId.toString()).claim("token_use", "access")
                .claim("role", credential.role().name()).build();
        String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new TokenResponse(access, refresh, "Bearer", java.time.Duration.between(now, accessExpiresAt).toSeconds(), expiresAt,
                new CurrentUserResponse(credential.userId(), credential.login(), credential.fullName(),
                        credential.role(), credential.mustChangePassword()));
    }

    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен", exception);
        }
    }
}
