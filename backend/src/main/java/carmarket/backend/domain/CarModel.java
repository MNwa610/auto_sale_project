package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "car_models")
public class CarModel extends CatalogEntry {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
}
