package carmarket.backend.service;

import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import carmarket.backend.domain.RoleCode;
import carmarket.backend.dto.AuditDetails;
import carmarket.backend.dto.AuditDetails.Field;
import carmarket.backend.dto.PageResponse;
import carmarket.backend.error.ApiFailure;
import carmarket.backend.repository.AuthRepository;
import carmarket.backend.security.PasswordPolicy;
import carmarket.backend.security.SessionUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AdminUserService {
    public record Account(long id, String login, String fullName, RoleCode role, boolean enabled,
                          boolean mustChangePassword, long version) { }

    private static final String ACCOUNTS = """
            SELECT u.id, c.login, u.full_name, r.code, u.enabled, u.must_change_password, u.version
            FROM users u JOIN roles r ON r.id=u.role_id LEFT JOIN user_credentials c ON c.user_id=u.id
            """;
    private static final RowMapper<Account> ACCOUNT = (rs, row) -> new Account(rs.getLong("id"),
            rs.getString("login"), rs.getString("full_name"), RoleCode.valueOf(rs.getString("code")),
            rs.getBoolean("enabled"), rs.getBoolean("must_change_password"), rs.getLong("version"));

    private final JdbcTemplate jdbc;
    private final AuthRepository auth;
    private final AuditService audit;
    private final PasswordPolicy policy;
    private final PasswordEncoder passwords;
    private final Clock clock;

    public AdminUserService(JdbcTemplate jdbc, AuthRepository auth, AuditService audit, PasswordPolicy policy,
                            PasswordEncoder passwords, Clock clock) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.audit = audit;
        this.policy = policy;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<Account> list(int page, int size) {
        long count = jdbc.queryForObject("SELECT count(*) FROM users", Long.class);
        List<Account> accounts = jdbc.query(ACCOUNTS + " ORDER BY u.id LIMIT ? OFFSET ?", ACCOUNT, size,
                (long) page * size);
        return new PageResponse<>(accounts, page, size, count, (count + size - 1) / size);
    }

    public Account create(SessionUser actor, String login, String fullName, RoleCode role, String password) {
        String normalized = PasswordPolicy.normalizeLogin(login);
        if (normalized.isBlank() || normalized.length() > 50 || normalized.codePoints().anyMatch(Character::isISOControl)
                || fullName.isBlank() || fullName.codePoints().anyMatch(Character::isISOControl)) {
            throw ApiFailure.invalid("Некорректный логин или имя пользователя.");
        }
        policy.validate(password);
        lockAdministration(actor);
        if (auth.credential(normalized).isPresent()) {
            throw ApiFailure.conflict("LOGIN_EXISTS", "Этот логин уже используется.");
        }
        auth.lockLogin(normalized);
        auth.clearAttempts(normalized);
        long id = jdbc.queryForObject("""
                INSERT INTO users(full_name, role_id) SELECT ?, id FROM roles WHERE code=? RETURNING id
                """, Long.class, fullName.strip(), role.name());
        jdbc.update("INSERT INTO user_credentials(user_id, login, password_hash) VALUES (?, ?, ?)",
                id, normalized, passwords.encode(password));
        record(id, AuditAction.CREATED, Set.of(Field.ROLE, Field.ENABLED, Field.MUST_CHANGE_PASSWORD), null, role, true);
        return get(id);
    }

    public Account update(SessionUser actor, long id, RoleCode role, boolean enabled, long version) {
        lockAdministration(actor);
        Account account = lockTarget(id);
        requireVersion(account, version);
        var fields = EnumSet.noneOf(Field.class);
        if (role != account.role()) fields.add(Field.ROLE);
        if (enabled != account.enabled()) fields.add(Field.ENABLED);
        if (fields.isEmpty()) return account;
        if (actor.id() == id) throw ApiFailure.conflict("SELF_ADMIN_CHANGE", "Нельзя менять собственную роль или блокировку.");
        jdbc.update("""
                UPDATE users SET role_id=(SELECT id FROM roles WHERE code=?), enabled=?,
                    version=version+1, updated_at=? WHERE id=?
                """, role.name(), enabled, Timestamp.from(clock.instant()), id);
        int revoked = auth.revokeAll(id, clock.instant());
        record(id, AuditAction.UPDATED, fields, revoked, role, null);
        return get(id);
    }

    public Account resetPassword(SessionUser actor, long id, String password, long version) {
        policy.validate(password);
        lockAdministration(actor);
        if (actor.id() == id) throw ApiFailure.conflict("SELF_ADMIN_CHANGE", "Используйте обычную смену своего пароля.");
        // Login locks precede user locks, as in AuthService.login.
        String login = get(id).login();
        if (login == null) throw ApiFailure.conflict("NO_LOGIN", "У пользователя отсутствует логин.");
        auth.lockLogin(login);
        Account target = lockTarget(id);
        requireVersion(target, version);
        String previous = auth.credential(login).orElseThrow().hash();
        if (previous != null && passwords.matches(password, previous)) {
            throw ApiFailure.invalid("Временный пароль должен отличаться от текущего.");
        }
        jdbc.update("UPDATE user_credentials SET password_hash=? WHERE user_id=?", passwords.encode(password), id);
        jdbc.update("UPDATE users SET must_change_password=TRUE, version=version+1, updated_at=? WHERE id=?",
                Timestamp.from(clock.instant()), id);
        auth.clearAttempts(login);
        int revoked = auth.revokeAll(id, clock.instant());
        record(id, AuditAction.TEMPORARY_PASSWORD_SET, Set.of(Field.PASSWORD, Field.MUST_CHANGE_PASSWORD),
                revoked, null, true);
        return get(id);
    }

    public int revokeSessions(SessionUser actor, long id) {
        lockAdministration(actor);
        lockTarget(id);
        int revoked = auth.revokeAll(id, clock.instant());
        record(id, AuditAction.SESSIONS_REVOKED, Set.of(), revoked, null, null);
        return revoked;
    }

    private void lockAdministration(SessionUser actor) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(820031751)");
        auth.lockUser(actor.id());
        if (auth.sessionUser(actor.id(), actor.sessionId(), clock.instant())
                .filter(user -> user.role() == RoleCode.ADMIN && !user.mustChangePassword()).isEmpty()) {
            throw new AccessDeniedException("Требуется действующая сессия администратора.");
        }
    }

    private Account get(long id) {
        return jdbc.query(ACCOUNTS + " WHERE u.id=?", ACCOUNT, id).stream().findFirst().orElseThrow(ApiFailure::notFound);
    }

    private Account lockTarget(long id) {
        if (!auth.lockUser(id)) throw ApiFailure.notFound();
        return get(id);
    }

    private static void requireVersion(Account account, long version) {
        if (account.version() != version) {
            throw ApiFailure.conflict("VERSION_CONFLICT", "Запись изменена. Обновите список пользователей.");
        }
    }

    private void record(long id, AuditAction action, Set<Field> fields, Integer revoked, RoleCode role, Boolean temporary) {
        audit.recordAsCurrentUser(action, AuditTarget.user(id), new AuditDetails(fields, revoked, role, temporary));
    }
}
