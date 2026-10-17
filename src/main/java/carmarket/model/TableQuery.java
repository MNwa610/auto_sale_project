package carmarket.model;

import carmarket.enums.RequestStatus;
import carmarket.enums.SortField;

public record TableQuery(String text, RequestStatus status, String minPrice, String maxPrice,
                         SortField sortField, boolean ascending) {
    public static TableQuery empty() {
        return new TableQuery("", null, "", "", SortField.DEFAULT, true);
    }
}
