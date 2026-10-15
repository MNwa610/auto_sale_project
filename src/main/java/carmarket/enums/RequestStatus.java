package carmarket.enums;

public enum RequestStatus {
    NEW("Новая"),
    IN_PROGRESS("В работе"),
    APPROVED("Одобрена"),
    REJECTED("Отклонена"),
    CANCELLED("Отменена");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
