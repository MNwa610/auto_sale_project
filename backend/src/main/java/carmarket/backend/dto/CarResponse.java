package carmarket.backend.dto;

import carmarket.backend.domain.CarStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CarResponse(UUID id, String externalCode, SellerResponse seller,
                          CatalogRef brand, CatalogRef model, int year, int mileage, BigDecimal price,
                          String vin, CatalogRef bodyType, CatalogRef transmission, CatalogRef fuelType,
                          BigDecimal engineVolume, String description, CarStatus status, boolean certified,
                          Set<String> tags, int ageYears, boolean archived, long version,
                          Instant createdAt, Instant updatedAt) { }
