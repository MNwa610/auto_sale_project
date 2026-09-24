package carmarket.model;

import java.math.BigDecimal;

public class CarSpecification {

    private Long carId;
    private String vin;
    private Long bodyTypeId;
    private String bodyType;
    private Long transmissionId;
    private String transmission;
    private Long fuelTypeId;
    private String fuelType;
    private BigDecimal engineVolume;
    private String description;

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public Long getBodyTypeId() {
        return bodyTypeId;
    }

    public void setBodyTypeId(Long bodyTypeId) {
        this.bodyTypeId = bodyTypeId;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public Long getTransmissionId() {
        return transmissionId;
    }

    public void setTransmissionId(Long transmissionId) {
        this.transmissionId = transmissionId;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public Long getFuelTypeId() {
        return fuelTypeId;
    }

    public void setFuelTypeId(Long fuelTypeId) {
        this.fuelTypeId = fuelTypeId;
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
}
