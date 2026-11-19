package carmarket.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "car_specifications")
public class CarSpecification {
    @Id
    @Column(name = "car_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "car_id")
    private Car car;

    @Column(length = 17)
    private String vin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "body_type_id", nullable = false)
    private BodyType bodyType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transmission_id", nullable = false)
    private Transmission transmission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fuel_type_id", nullable = false)
    private FuelType fuelType;

    @Column(name = "engine_volume", precision = 3, scale = 1)
    private BigDecimal engineVolume;

    @Column(columnDefinition = "text")
    private String description;

    public Long getId() { return id; }
    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public BodyType getBodyType() { return bodyType; }
    public void setBodyType(BodyType bodyType) { this.bodyType = bodyType; }
    public Transmission getTransmission() { return transmission; }
    public void setTransmission(Transmission transmission) { this.transmission = transmission; }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public BigDecimal getEngineVolume() { return engineVolume; }
    public void setEngineVolume(BigDecimal engineVolume) { this.engineVolume = engineVolume; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
