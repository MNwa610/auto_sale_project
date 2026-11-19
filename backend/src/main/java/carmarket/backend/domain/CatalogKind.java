package carmarket.backend.domain;

import carmarket.backend.error.ApiFailure;

public enum CatalogKind {
    BRANDS("brands", Brand.class, 50), MODELS("models", CarModel.class, 100),
    BODY_TYPES("body-types", BodyType.class, 30), TRANSMISSIONS("transmissions", Transmission.class, 30),
    FUEL_TYPES("fuel-types", FuelType.class, 30);

    private final String path;
    private final Class<? extends CatalogEntry> entityClass;
    private final int nameLimit;
    CatalogKind(String path, Class<? extends CatalogEntry> entityClass, int nameLimit) {
        this.path = path;
        this.entityClass = entityClass;
        this.nameLimit = nameLimit;
    }
    public Class<? extends CatalogEntry> entityClass() { return entityClass; }
    public int nameLimit() { return nameLimit; }
    public static CatalogKind fromPath(String path) {
        for (CatalogKind kind : values()) if (kind.path.equals(path)) return kind;
        throw ApiFailure.notFound();
    }
    public CatalogEntry newEntry() {
        return switch (this) {
            case BRANDS -> new Brand();
            case MODELS -> new CarModel();
            case BODY_TYPES -> new BodyType();
            case TRANSMISSIONS -> new Transmission();
            case FUEL_TYPES -> new FuelType();
        };
    }
}
