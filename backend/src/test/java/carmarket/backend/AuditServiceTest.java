package carmarket.backend;

import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import carmarket.backend.dto.AuditDetails;
import carmarket.backend.security.SessionAuthentication;
import carmarket.backend.security.SessionUser;
import carmarket.backend.domain.RoleCode;
import carmarket.backend.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AuditServiceTest extends PostgresTestSupport {
    @Autowired private AuditService audit;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private ObjectMapper mapper;
    @Autowired private Clock clock;

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void auditCannotStartAnIndependentTransaction() {
        UUID target = UUID.randomUUID();
        assertThatThrownBy(() -> audit.recordSystem(AuditAction.CREATED, AuditTarget.car(target), AuditDetails.none()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(count(target.toString())).isZero();
    }

    @Test
    void changeAndAuditCommitTogetherWithActorSnapshotAndTime() throws Exception {
        long userId = user();
        String login = jdbc.queryForObject("SELECT login FROM user_credentials WHERE user_id=?", String.class, userId);
        Instant before = clock.instant();
        transactions.executeWithoutResult(status -> {
            jdbc.update("UPDATE users SET enabled=FALSE WHERE id=?", userId);
            audit.recordAsUser(userId, AuditAction.UPDATED, AuditTarget.user(userId),
                    new AuditDetails(Set.of(AuditDetails.Field.ENABLED), null, null, null));
        });
        var event = jdbc.queryForMap("SELECT * FROM audit_events WHERE entity_id=?", Long.toString(userId));
        assertThat(event.get("actor_id")).isEqualTo(userId);
        assertThat(event.get("actor_login")).isEqualTo(login);
        assertThat(event.get("entity_type")).isEqualTo("USER");
        assertThat(event.get("action")).isEqualTo("UPDATED");
        assertThat(mapper.readTree(event.get("details").toString()).get("changedFields").get(0).asText()).isEqualTo("ENABLED");
        assertThat(((java.sql.Timestamp) event.get("created_at")).toInstant()).isBetween(before.minusNanos(1000), clock.instant().plusNanos(1000));
        assertThat(jdbc.queryForObject("SELECT enabled FROM users WHERE id=?", Boolean.class, userId)).isFalse();
    }

    @Test
    void laterFailureRollsBackBusinessChangeAndAudit() {
        long userId = user();
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            jdbc.update("UPDATE users SET enabled=FALSE WHERE id=?", userId);
            audit.recordAsUser(userId, AuditAction.UPDATED, AuditTarget.user(userId), AuditDetails.none());
            throw new IllegalStateException("Тестовый отказ после записи события");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count(Long.toString(userId))).isZero();
        assertThat(jdbc.queryForObject("SELECT enabled FROM users WHERE id=?", Boolean.class, userId)).isTrue();
    }

    @Test
    void databaseAuditFailureRollsBackBusinessChange() {
        long userId = user();
        UUID rejected = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            // Ограничение существует только внутри этой тестовой транзакции и откатывается вместе с ней.
            jdbc.execute("ALTER TABLE audit_events ADD CONSTRAINT reject_test_audit "
                    + "CHECK (entity_id <> 'ffffffff-ffff-ffff-ffff-ffffffffffff') NOT VALID");
            jdbc.update("UPDATE users SET enabled=FALSE WHERE id=?", userId);
            audit.recordSystem(AuditAction.UPDATED, AuditTarget.car(rejected), AuditDetails.none());
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT enabled FROM users WHERE id=?", Boolean.class, userId)).isTrue();
        assertThat(count(rejected.toString())).isZero();
    }

    @Test
    void swallowedAuditFailureStillPreventsCommit() {
        long userId = user();
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            jdbc.update("UPDATE users SET enabled=FALSE WHERE id=?", userId);
            try {
                audit.recordAsUser(Long.MAX_VALUE, AuditAction.UPDATED, AuditTarget.user(userId), AuditDetails.none());
            } catch (RuntimeException ignored) {
                // Даже ошибочный вызывающий код не должен закоммитить изменение без обязательного аудита.
            }
        })).isInstanceOf(UnexpectedRollbackException.class);
        assertThat(count(Long.toString(userId))).isZero();
        assertThat(jdbc.queryForObject("SELECT enabled FROM users WHERE id=?", Boolean.class, userId)).isTrue();
    }

    @Test
    void currentActorComesFromSecurityContextAndLoginComesFromDatabase() {
        long userId = user();
        UUID target = UUID.randomUUID();
        String login = jdbc.queryForObject("SELECT login FROM user_credentials WHERE user_id=?", String.class, userId);
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication(
                new SessionUser(userId, UUID.randomUUID(), "stale-client-login", "Тест", RoleCode.VIEWER, false)));
        transactions.executeWithoutResult(status ->
                audit.recordAsCurrentUser(AuditAction.UPDATED, AuditTarget.car(target), AuditDetails.none()));
        assertThat(jdbc.queryForObject("SELECT actor_login FROM audit_events WHERE entity_id=?", String.class, target.toString()))
                .isEqualTo(login);
    }

    @Test
    void absentActorCannotSilentlyBecomeSystem() {
        UUID target = UUID.randomUUID();
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> transactions.executeWithoutResult(status ->
                audit.recordAsCurrentUser(AuditAction.UPDATED, AuditTarget.car(target), AuditDetails.none())))
                .isInstanceOf(IllegalStateException.class);
        assertThat(count(target.toString())).isZero();
    }

    @Test
    void actorLoginAndTargetSurviveDeletionOfActor() {
        long userId = user();
        String login = jdbc.queryForObject("SELECT login FROM user_credentials WHERE user_id=?", String.class, userId);
        UUID target = UUID.randomUUID();
        transactions.executeWithoutResult(status ->
                audit.recordAsUser(userId, AuditAction.DELETED, AuditTarget.car(target), AuditDetails.none()));
        jdbc.update("DELETE FROM users WHERE id=?", userId);
        var event = jdbc.queryForMap("SELECT actor_id, actor_login, entity_id FROM audit_events WHERE entity_id=?", target.toString());
        assertThat(event.get("actor_id")).isNull();
        assertThat(event.get("actor_login")).isEqualTo(login);
        assertThat(event.get("entity_id")).isEqualTo(target.toString());
    }

    private long user() {
        Long id = jdbc.queryForObject("INSERT INTO users(full_name, role_id) "
                + "SELECT 'Автор аудита', id FROM roles WHERE code='VIEWER' RETURNING id", Long.class);
        jdbc.update("INSERT INTO user_credentials(user_id, login) VALUES (?, ?)", id, "audit-" + UUID.randomUUID());
        return id;
    }

    private long count(String target) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE entity_id=?", Long.class, target);
    }
}
