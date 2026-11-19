package carmarket.backend.dto;

import carmarket.backend.domain.CatalogEntry;
import carmarket.backend.domain.CarModel;

public record CatalogRef(long id, String name, boolean enabled) {
    public static CatalogRef from(CatalogEntry item) {
        return new CatalogRef(item.getId(), item.getName(), item.isEnabled()
                && (!(item instanceof CarModel model) || model.getBrand().isEnabled()));
    }
}
