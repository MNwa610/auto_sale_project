package carmarket.backend.dto;

import carmarket.backend.domain.RoleCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Только метаданные операции. Здесь нет свободного текста, DTO, паролей или токенов. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AuditDetails(Set<Field> changedFields, Integer revokedSessions,
                           RoleCode role, Boolean mustChangePassword) {
    public enum Field { PASSWORD, MUST_CHANGE_PASSWORD, ENABLED, ROLE, NAME,
        EXTERNAL_CODE, SELLER, MODEL, YEAR, MILEAGE, PRICE, VIN, BODY_TYPE, TRANSMISSION,
        FUEL_TYPE, ENGINE_VOLUME, DESCRIPTION, STATUS, CERTIFIED, TAGS }

    public AuditDetails {
        changedFields = changedFields == null || changedFields.isEmpty() ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(changedFields));
        if (revokedSessions != null && revokedSessions < 0) {
            throw new IllegalArgumentException("Число отозванных сессий не может быть отрицательным");
        }
    }

    public static AuditDetails none() { return new AuditDetails(Set.of(), null, null, null); }
    public static AuditDetails changed(Set<Field> fields) { return new AuditDetails(fields, null, null, null); }
    public static AuditDetails sessionsRevoked(int count) { return new AuditDetails(Set.of(), count, null, null); }
    public static AuditDetails passwordChanged(int count) {
        return new AuditDetails(Set.of(Field.PASSWORD, Field.MUST_CHANGE_PASSWORD), count, null, false);
    }
    public static AuditDetails adminCreated() {
        return new AuditDetails(Set.of(Field.ROLE, Field.ENABLED, Field.MUST_CHANGE_PASSWORD), null, RoleCode.ADMIN, true);
    }
}
