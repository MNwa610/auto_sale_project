package carmarket.enums;

public enum SortField {
    DEFAULT("По умолчанию"),
    PRICE("Цена автомобиля"),
    MILEAGE("Пробег"),
    DATE("Дата создания"),
    STATUS("Статус");

    private final String displayName;

    SortField(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
