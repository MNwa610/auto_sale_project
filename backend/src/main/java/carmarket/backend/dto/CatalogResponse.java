package carmarket.backend.dto;

import carmarket.backend.domain.CatalogEntry;
import carmarket.backend.domain.CarModel;
import java.time.Instant;

public record CatalogResponse(long id, String name, boolean enabled, boolean effectiveEnabled, Long brandId,
                              long version, Instant createdAt, Instant updatedAt) {
    public static CatalogResponse from(CatalogEntry item) {
        CarModel model = item instanceof CarModel value ? value : null;
        return new CatalogResponse(item.getId(), item.getName(), item.isEnabled(),
                item.isEnabled() && (model == null || model.getBrand().isEnabled()),
                model == null ? null : model.getBrand().getId(), item.getVersion(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
