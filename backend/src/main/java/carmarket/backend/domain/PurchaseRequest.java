package carmarket.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_requests")
public class PurchaseRequest extends VersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.NEW;

    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}
