package carmarket.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cars")
public class Car extends VersionedEntity {
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "external_code", nullable = false, length = 64)
    private String externalCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserAccount seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private CarModel model;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int mileage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarStatus status = CarStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean certified = false;

    @Column(nullable = false)
    private boolean archived = false;

    @ElementCollection
    @CollectionTable(name = "car_tags", joinColumns = @JoinColumn(name = "car_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @OneToOne(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private CarSpecification specification;

    public UUID getPublicId() { return publicId; }
    public String getExternalCode() { return externalCode; }
    public void setExternalCode(String externalCode) { this.externalCode = externalCode; }
    public UserAccount getSeller() { return seller; }
    public void setSeller(UserAccount seller) { this.seller = seller; }
    public CarModel getModel() { return model; }
    public void setModel(CarModel model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public CarStatus getStatus() { return status; }
    public void setStatus(CarStatus status) { this.status = status; }
    public boolean isCertified() { return certified; }
    public void setCertified(boolean certified) { this.certified = certified; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public CarSpecification getSpecification() { return specification; }
    public void setSpecification(CarSpecification specification) { this.specification = specification; }
}
