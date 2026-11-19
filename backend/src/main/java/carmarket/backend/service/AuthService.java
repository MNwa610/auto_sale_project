package carmarket.backend.service;

import carmarket.backend.dto.*;
import carmarket.backend.error.AuthFailure;
import carmarket.backend.repository.AuthRepository;
import carmarket.backend.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
// Неудачный вход и обнаружение повтора refresh должны сохранить счётчик/отзыв.
@Transactional(noRollbackFor = AuthFailure.class)
public class AuthService {
    private final AuthRepository repository;
    private final PasswordEncoder passwords;
    private final PasswordPolicy policy;
    private final AuthProperties properties;
    private final TokenService tokens;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(AuthRepository repository, PasswordEncoder passwords, PasswordPolicy policy,
                       AuthProperties properties, TokenService tokens, Clock clock) {
        this.repository = repository;
        this.passwords = passwords;
        this.policy = policy;
        this.properties = properties;
        this.tokens = tokens;
        this.clock = clock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public TokenResponse login(LoginRequest request) {
        validatePasswordInput(request.password());
        String login = PasswordPolicy.normalizeLogin(request.login());
        repository.lockLogin(login);
        Instant now = clock.instant();
        AuthRepository.Attempt attempt = repository.attempt(login).orElse(null);
        if (attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(now)) {
            throw locked();
        }
        AuthRepository.Credential credential = repository.credential(login).orElse(null);
        if (credential != null) {
            repository.lockUser(credential.userId());
            credential = repository.credential(login).orElse(null);
        }
        String hash = credential == null || credential.hash() == null ? dummyHash : credential.hash();
        boolean matches = matches(request.password(), hash);
        if (!matches || credential == null || !credential.enabled() || credential.hash() == null) {
            boolean reset = attempt == null || attempt.lastFailedAt().isBefore(now.minus(properties.failureWindow()))
                    || (attempt.lockedUntil() != null && !attempt.lockedUntil().isAfter(now));
            int failures = reset ? 1 : attempt.count() + 1;
            boolean lock = failures >= properties.maxFailedAttempts();
            repository.saveAttempt(login, failures, now, lock ? now.plus(properties.lockDuration()) : null);
            if (lock) throw locked();
            throw invalidCredentials();
        }
        repository.clearAttempts(login);
        return tokens.issue(credential, null, null);
    }

    public TokenResponse refresh(RefreshRequest request) {
        validateRefreshInput(request.refreshToken());
        String hash = TokenService.hash(request.refreshToken());
        AuthRepository.RefreshSession session = repository.refreshSession(hash).orElseThrow(AuthService::invalidToken);
        repository.lockUser(session.userId());
        // Вход, обновление, выход и смена пароля блокируют users в одинаковом порядке.
        session = repository.refreshSession(hash).orElseThrow(AuthService::invalidToken);
        if (session.revokedAt() != null) {
            repository.revokeFamily(session.userId(), session.familyId(), clock.instant());
            throw invalidToken();
        }
        SessionUser user = repository.sessionUser(session.userId(), session.id(), clock.instant())
                .orElseThrow(AuthService::invalidToken);
        AuthRepository.Credential credential = repository.credential(user.login()).orElseThrow(AuthService::invalidToken);
        TokenResponse response = tokens.issue(credential, session.familyId(), session.expiresAt());
        UUID newSession = repository.refreshSession(TokenService.hash(response.refreshToken())).orElseThrow().id();
        repository.rotate(session.id(), newSession, clock.instant());
        return response;
    }

    public void logout(RefreshRequest request) {
        validateRefreshInput(request.refreshToken());
        repository.refreshSession(TokenService.hash(request.refreshToken())).ifPresent(session -> {
            repository.lockUser(session.userId());
            repository.revokeFamily(session.userId(), session.familyId(), clock.instant());
        });
    }

    public TokenResponse changePassword(SessionUser user, ChangePasswordRequest request) {
        validatePasswordInput(request.currentPassword());
        policy.validate(request.newPassword());
        repository.lockUser(user.id());
        SessionUser current = repository.sessionUser(user.id(), user.sessionId(), clock.instant())
                .orElseThrow(AuthService::invalidToken);
        AuthRepository.Credential credential = repository.credential(current.login()).orElseThrow(AuthService::invalidToken);
        if (!matches(request.currentPassword(), credential.hash())) throw invalidCredentials();
        if (matches(request.newPassword(), credential.hash())) {
            throw new AuthFailure(HttpStatus.BAD_REQUEST, "PASSWORD_REUSE", "Новый пароль должен отличаться от текущего.");
        }
        repository.changePassword(user.id(), passwords.encode(request.newPassword()), clock.instant());
        repository.revokeAll(user.id(), clock.instant());
        return tokens.issue(repository.credential(current.login()).orElseThrow(), null, null);
    }

    private boolean matches(String raw, String hash) {
        return raw.getBytes(StandardCharsets.UTF_8).length <= 72 && passwords.matches(raw, hash);
    }

    // Bean Validation сохраняет rejectedValue в исключении и может напечатать его
    // в DEBUG-логе Spring. Секретные поля проверяем без включения исходного значения.
    private static void validatePasswordInput(String password) {
        if (password == null || password.isBlank() || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthFailure(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Некорректный формат пароля.");
        }
    }

    private static void validateRefreshInput(String token) {
        if (token == null || token.length() != 43 || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new AuthFailure(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Некорректный формат refresh-токена.");
        }
    }

    private static AuthFailure invalidCredentials() {
        return new AuthFailure(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Неверный логин или пароль.");
    }
    private static AuthFailure invalidToken() {
        return new AuthFailure(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Сессия недействительна. Войдите снова.");
    }
    private static AuthFailure locked() {
        return new AuthFailure(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED", "Слишком много попыток входа. Повторите позже.");
    }
}
