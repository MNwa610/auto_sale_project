package carmarket.backend.repository;

import carmarket.backend.domain.RoleCode;
import carmarket.backend.security.SessionUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Короткие параметризованные запросы; транзакции и порядок блокировок задаёт сервис. */
@Repository
public class AuthRepository {
    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Credential(long userId, String login, String hash, String fullName,
                             RoleCode role, boolean enabled, boolean mustChangePassword) {
        @Override public String toString() { return "Credential[redacted]"; }
    }
    public record RefreshSession(UUID id, long userId, UUID familyId, Instant expiresAt, Instant revokedAt) { }
    public record Attempt(int count, Instant lastFailedAt, Instant lockedUntil) { }

    public Optional<Credential> credential(String login) {
        return jdbc.query("SELECT u.id, c.login, c.password_hash, u.full_name, r.code, u.enabled, u.must_change_password "
                + "FROM user_credentials c JOIN users u ON u.id=c.user_id JOIN roles r ON r.id=u.role_id "
                + "WHERE c.login=?", (rs, row) -> new Credential(rs.getLong("id"), rs.getString("login"),
                rs.getString("password_hash"), rs.getString("full_name"), RoleCode.valueOf(rs.getString("code")),
                rs.getBoolean("enabled"), rs.getBoolean("must_change_password")), login).stream().findFirst();
    }

    public void lockLogin(String login) {
        // Сериализация счётчика работает и для несуществующих логинов, между экземплярами backend.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", login);
    }

    public boolean lockUser(long userId) {
        return !jdbc.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", userId).isEmpty();
    }

    public Optional<Attempt> attempt(String login) {
        return jdbc.query("SELECT failed_attempts, last_failed_at, locked_until FROM login_attempts WHERE login=?",
                (rs, row) -> new Attempt(rs.getInt(1), instant(rs, "last_failed_at"), instant(rs, "locked_until")),
                login).stream().findFirst();
    }

    public void saveAttempt(String login, int count, Instant now, Instant lockedUntil) {
        jdbc.update("INSERT INTO login_attempts(login, failed_attempts, last_failed_at, locked_until) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (login) DO UPDATE SET failed_attempts=EXCLUDED.failed_attempts, "
                        + "last_failed_at=EXCLUDED.last_failed_at, locked_until=EXCLUDED.locked_until",
                login, count, Timestamp.from(now), timestamp(lockedUntil));
    }

    public void clearAttempts(String login) { jdbc.update("DELETE FROM login_attempts WHERE login=?", login); }

    public Optional<RefreshSession> refreshSession(String hash) {
        return jdbc.query("SELECT id, user_id, family_id, expires_at, revoked_at FROM refresh_sessions WHERE token_hash=?",
                (rs, row) -> new RefreshSession(rs.getObject("id", UUID.class), rs.getLong("user_id"),
                        rs.getObject("family_id", UUID.class), instant(rs, "expires_at"), instant(rs, "revoked_at")),
                hash).stream().findFirst();
    }

    public void createSession(UUID id, long userId, UUID familyId, String hash, Instant now, Instant expiresAt) {
        jdbc.update("INSERT INTO refresh_sessions(id, user_id, family_id, token_hash, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)", id, userId, familyId, hash, Timestamp.from(now), Timestamp.from(expiresAt));
    }

    public void rotate(UUID oldId, UUID newId, Instant now) {
        jdbc.update("UPDATE refresh_sessions SET revoked_at=?, replaced_by=? WHERE id=?",
                Timestamp.from(now), newId, oldId);
    }

    public int revokeFamily(long userId, UUID familyId, Instant now) {
        return jdbc.update("UPDATE refresh_sessions SET revoked_at=? WHERE user_id=? AND family_id=? AND revoked_at IS NULL",
                Timestamp.from(now), userId, familyId);
    }

    public int revokeAll(long userId, Instant now) {
        return jdbc.update("UPDATE refresh_sessions SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL",
                Timestamp.from(now), userId);
    }

    public Optional<SessionUser> sessionUser(long userId, UUID sessionId, Instant now) {
        return jdbc.query("SELECT u.id, c.login, u.full_name, r.code, u.must_change_password FROM users u "
                + "JOIN user_credentials c ON c.user_id=u.id JOIN roles r ON r.id=u.role_id "
                + "JOIN refresh_sessions s ON s.user_id=u.id "
                + "WHERE u.id=? AND s.id=? AND u.enabled AND c.password_hash IS NOT NULL "
                + "AND s.revoked_at IS NULL AND s.expires_at>?",
                (rs, row) -> new SessionUser(rs.getLong("id"), sessionId, rs.getString("login"),
                        rs.getString("full_name"), RoleCode.valueOf(rs.getString("code")),
                        rs.getBoolean("must_change_password")), userId, sessionId, Timestamp.from(now)).stream().findFirst();
    }

    public void changePassword(long userId, String hash, Instant now) {
        jdbc.update("UPDATE user_credentials SET password_hash=? WHERE user_id=?", hash, userId);
        jdbc.update("UPDATE users SET must_change_password=FALSE, version=version+1, updated_at=? WHERE id=?",
                Timestamp.from(now), userId);
    }

    public boolean hasActiveAdmin() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM users u "
                + "JOIN roles r ON r.id=u.role_id JOIN user_credentials c ON c.user_id=u.id "
                + "WHERE r.code='ADMIN' AND u.enabled AND c.password_hash IS NOT NULL)", Boolean.class));
    }

    public long createAdmin(String login, String hash) {
        Long id = jdbc.queryForObject("INSERT INTO users(full_name, role_id, must_change_password) "
                + "SELECT 'Администратор системы', id, TRUE FROM roles WHERE code='ADMIN' RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_credentials(user_id, login, password_hash) VALUES (?, ?, ?)", id, login, hash);
        return java.util.Objects.requireNonNull(id);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
    private static Timestamp timestamp(Instant instant) { return instant == null ? null : Timestamp.from(instant); }
}
