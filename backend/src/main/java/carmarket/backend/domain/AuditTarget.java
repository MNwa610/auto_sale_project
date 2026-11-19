package carmarket.backend.domain;

import java.util.Objects;
import java.util.UUID;

/** Идентификаторы типизированы: произвольную строку или секрет передать нельзя. */
public record AuditTarget(Type type, Long numericId, UUID publicId) {
    public enum Type { USER, CAR, REFRESH_SESSION }

    public AuditTarget {
        Objects.requireNonNull(type, "Тип объекта аудита обязателен");
        boolean valid = type == Type.USER
                ? numericId != null && numericId > 0 && publicId == null
                : publicId != null && numericId == null;
        if (!valid) throw new IllegalArgumentException("Некорректный идентификатор объекта аудита");
    }

    public static AuditTarget user(long id) { return new AuditTarget(Type.USER, id, null); }
    public static AuditTarget car(UUID id) { return new AuditTarget(Type.CAR, null, id); }
    public static AuditTarget session(UUID id) { return new AuditTarget(Type.REFRESH_SESSION, null, id); }
    public String entityId() { return numericId == null ? publicId.toString() : numericId.toString(); }
}
