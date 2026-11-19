package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "transmissions")
@AttributeOverride(name = "name", column = @Column(name = "name", nullable = false, length = 30))
public class Transmission extends CatalogEntry {
}
