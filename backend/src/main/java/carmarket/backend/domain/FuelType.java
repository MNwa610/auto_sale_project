package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "fuel_types")
@AttributeOverride(name = "name", column = @Column(name = "name", nullable = false, length = 30))
public class FuelType extends CatalogEntry {
}
