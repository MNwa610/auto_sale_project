package carmarket.backend.domain;

import java.util.Objects;
import java.util.UUID;

/** Идентификаторы типизированы: произвольную строку или секрет передать нельзя. */
public record AuditTarget(Type type, Long numericId, UUID publicId) {
    public enum Type { USER, CAR, REFRESH_SESSION, BRAND, CAR_MODEL, BODY_TYPE, TRANSMISSION, FUEL_TYPE }

    public AuditTarget {
        Objects.requireNonNull(type, "Тип объекта аудита обязателен");
        boolean numeric = type != Type.CAR && type != Type.REFRESH_SESSION;
        boolean valid = numeric
                ? numericId != null && numericId > 0 && publicId == null
                : publicId != null && numericId == null;
        if (!valid) throw new IllegalArgumentException("Некорректный идентификатор объекта аудита");
    }

    public static AuditTarget user(long id) { return new AuditTarget(Type.USER, id, null); }
    public static AuditTarget car(UUID id) { return new AuditTarget(Type.CAR, null, id); }
    public static AuditTarget session(UUID id) { return new AuditTarget(Type.REFRESH_SESSION, null, id); }
    public static AuditTarget catalog(CatalogKind kind, long id) {
        Type type = switch (kind) {
            case BRANDS -> Type.BRAND;
            case MODELS -> Type.CAR_MODEL;
            case BODY_TYPES -> Type.BODY_TYPE;
            case TRANSMISSIONS -> Type.TRANSMISSION;
            case FUEL_TYPES -> Type.FUEL_TYPE;
        };
        return new AuditTarget(type, id, null);
    }
    public String entityId() { return numericId == null ? publicId.toString() : numericId.toString(); }
}
