package carmarket.enums;

public enum CarStatus {
    AVAILABLE("Доступен"),
    RESERVED("Зарезервирован"),
    SOLD("Продан"),
    ARCHIVED("В архиве");

    private final String displayName;

    CarStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
