package carmarket.backend.service;

import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import carmarket.backend.dto.PageResponse;
import carmarket.backend.error.ApiFailure;
import carmarket.backend.security.PasswordPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReportingService {
    public record Totals(long count, long available, long reserved, long sold, long certified,
                         BigDecimal totalPrice, BigDecimal averagePrice) { }
    public record BrandCount(long id, String name, long count) { }
    public record Summary(Totals totals, List<BrandCount> brands) { }
    public record AuditEvent(long id, Long actorId, String actorLogin, String action, String entityType,
                             String entityId, JsonNode details, Instant createdAt) { }

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ReportingService(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public Summary summary(boolean archived) {
        var params = new MapSqlParameterSource("archived", archived);
        Totals totals = jdbc.queryForObject("""
                SELECT count(*), count(*) FILTER (WHERE status='AVAILABLE'),
                    count(*) FILTER (WHERE status='RESERVED'), count(*) FILTER (WHERE status='SOLD'),
                    count(*) FILTER (WHERE certified), coalesce(sum(price), 0), round(avg(price), 2)
                FROM cars WHERE archived=:archived
                """, params, (rs, row) -> new Totals(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getLong(4), rs.getLong(5), rs.getBigDecimal(6), rs.getBigDecimal(7)));
        List<BrandCount> brands = jdbc.query("""
                SELECT b.id, b.name, count(*) AS car_count
                FROM cars c JOIN car_models m ON m.id=c.model_id JOIN brands b ON b.id=m.brand_id
                WHERE c.archived=:archived GROUP BY b.id, b.name ORDER BY car_count DESC, b.name, b.id
                """, params, (rs, row) -> new BrandCount(rs.getLong(1), rs.getString(2), rs.getLong(3)));
        return new Summary(totals, brands);
    }

    public PageResponse<AuditEvent> audit(String actorLogin, AuditAction action, AuditTarget.Type entityType,
            String entityId, Instant from, Instant to, int page, int size, String sort) {
        String order = switch (sort) {
            case "createdAt,asc" -> "created_at ASC, id ASC";
            case "createdAt,desc" -> "created_at DESC, id DESC";
            case "action,asc" -> "action ASC, id ASC";
            case "action,desc" -> "action DESC, id DESC";
            default -> throw ApiFailure.invalid("Недопустимая сортировка аудита.");
        };
        var params = new MapSqlParameterSource().addValue("limit", size).addValue("offset", (long) page * size);
        var filters = new ArrayList<String>();
        filter(filters, params, "actor_login = :login", "login",
                actorLogin == null || actorLogin.isBlank() ? null : PasswordPolicy.normalizeLogin(actorLogin));
        filter(filters, params, "action = :action", "action", action == null ? null : action.name());
        filter(filters, params, "entity_type = :type", "type", entityType == null ? null : entityType.name());
        filter(filters, params, "entity_id = :entity", "entity", entityId);
        filter(filters, params, "created_at >= :from", "from", from == null ? null : Timestamp.from(from));
        filter(filters, params, "created_at <= :to", "to", to == null ? null : Timestamp.from(to));
        String source = " FROM audit_events" + (filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters));
        long count = jdbc.queryForObject("SELECT count(*)" + source, params, Long.class);
        List<AuditEvent> events = jdbc.query("SELECT *" + source + " ORDER BY " + order
                + " LIMIT :limit OFFSET :offset", params, (rs, row) -> new AuditEvent(rs.getLong("id"),
                rs.getObject("actor_id", Long.class), rs.getString("actor_login"), rs.getString("action"),
                rs.getString("entity_type"), rs.getString("entity_id"), details(rs.getString("details")),
                rs.getTimestamp("created_at").toInstant()));
        return new PageResponse<>(events, page, size, count, (count + size - 1) / size);
    }

    private static void filter(List<String> filters, MapSqlParameterSource params, String predicate,
                               String name, Object value) {
        if (value != null) {
            filters.add(predicate);
            params.addValue(name, value);
        }
    }

    private JsonNode details(String json) {
        try {
            return mapper.readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Некорректный JSON журнала аудита", exception);
        }
    }
}
