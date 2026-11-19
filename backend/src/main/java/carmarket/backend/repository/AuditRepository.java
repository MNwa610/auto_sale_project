package carmarket.backend.repository;

import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;

/** Только добавление. Историю не редактируем и не удаляем через приложение. */
@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;
    public AuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void append(Long actorId, AuditAction action, AuditTarget target, String detailsJson, Instant time) {
        int inserted;
        if (actorId == null) {
            inserted = jdbc.update("INSERT INTO audit_events(action, entity_type, entity_id, details, created_at) "
                            + "VALUES (?, ?, ?, CAST(? AS jsonb), ?)",
                    action.name(), target.type().name(), target.entityId(), detailsJson, Timestamp.from(time));
        } else {
            // Снимок автора берём из БД, а не из HTTP-запроса или сохранённого JWT claim.
            inserted = jdbc.update("INSERT INTO audit_events(actor_id, actor_login, action, entity_type, entity_id, details, created_at) "
                            + "SELECT u.id, c.login, ?, ?, ?, CAST(? AS jsonb), ? FROM users u "
                            + "LEFT JOIN user_credentials c ON c.user_id=u.id WHERE u.id=?",
                    action.name(), target.type().name(), target.entityId(), detailsJson, Timestamp.from(time), actorId);
        }
        if (inserted != 1) throw new IllegalStateException("Не удалось сохранить событие аудита: автор не найден");
    }
}
