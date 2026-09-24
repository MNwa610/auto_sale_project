package carmarket.model;

import carmarket.enums.CarStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Car {

    private Long id;
    private Long sellerId;
    private String brand;
    private String model;
    private int year;
    private int mileage;
    private BigDecimal price;
    private String vin;
    private String bodyType;
    private String transmission;
    private String fuelType;
    private BigDecimal engineVolume;
    private String description;
    private CarStatus status;
    private LocalDateTime createdAt;

    public Car() {
    }

    public Car(Long sellerId, String brand, String model, int year, int mileage,
               BigDecimal price, String vin, String bodyType, String transmission,
               String fuelType, BigDecimal engineVolume, String description, CarStatus status) {
        this.sellerId = sellerId;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.mileage = mileage;
        this.price = price;
        this.vin = vin;
        this.bodyType = bodyType;
        this.transmission = transmission;
        this.fuelType = fuelType;
        this.engineVolume = engineVolume;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public BigDecimal getEngineVolume() {
        return engineVolume;
    }

    public void setEngineVolume(BigDecimal engineVolume) {
        this.engineVolume = engineVolume;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CarStatus getStatus() {
        return status;
    }

    public void setStatus(CarStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", sellerId=" + sellerId +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", year=" + year +
                ", mileage=" + mileage +
                ", price=" + price +
                ", status=" + status +
                '}';
    }
}
