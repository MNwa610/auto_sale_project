package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "brands")
@AttributeOverride(name = "name", column = @Column(name = "name", nullable = false, length = 50))
public class Brand extends CatalogEntry {
}
