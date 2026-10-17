package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.UserRole;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.RequestDetails;
import carmarket.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelatedDataServiceTest {
    private InMemoryUserRepository users;
    private InMemoryCarRepository cars;
    private InMemoryPurchaseRequestRepository requests;
    private PurchaseRequestService requestService;
    private CarService carService;
    private User seller;
    private User buyer;
    private User other;
    private Car toyota;
    private Car bmw;
    private PurchaseRequest buyerRequest;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        cars = new InMemoryCarRepository();
        requests = new InMemoryPurchaseRequestRepository();
        requestService = new PurchaseRequestService(requests, users, cars);
        carService = new CarService(cars, users);
        seller = new User("Продавец", "seller", "seller-password", UserRole.ADMIN);
        buyer = new User("Покупатель", "buyer", "buyer-password", UserRole.USER);
        other = new User("Другой пользователь", "other", "other-password", UserRole.USER);
        users.seed(seller);
        users.seed(buyer);
        users.seed(other);
        toyota = seedCar(seller, "Toyota", "Camry");
        bmw = seedCar(other, "BMW", "X5");
        buyerRequest = seedRequest(buyer, toyota, RequestStatus.NEW);
        seedRequest(other, toyota, RequestStatus.APPROVED);
        seedRequest(seller, bmw, RequestStatus.REJECTED);
    }

    @Test
    void requestDetails_shouldIncludeBuyerCarSellerAndRequestDates() {
        RequestDetails details = requestService.getDetails(buyerRequest.getId());
        assertThat(details.user().getId()).isEqualTo(buyer.getId());
        assertThat(details.user().getLogin()).isEqualTo("buyer");
        assertThat(details.user().getRole()).isEqualTo(UserRole.USER);
        assertThat(details.car().getId()).isEqualTo(toyota.getId());
        assertThat(details.car().getSellerFullName()).isEqualTo("Продавец");
        assertThat(details.car().getVin()).isEqualTo("JTNB11HK003000001");
        assertThat(details.request().getMessage()).isEqualTo("Хочу осмотреть автомобиль");
        assertThat(details.request().getCreatedAt()).isEqualTo(buyerRequest.getCreatedAt());
        assertThat(details.request().getUpdatedAt()).isEqualTo(buyerRequest.getUpdatedAt());
    }

    @Test
    void relatedLists_shouldDistinguishSellerFromBuyerAndIncludeFinalRequests() {
        assertThat(carService.findBySellerId(seller.getId())).extracting(Car::getId).containsExactly(toyota.getId());
        assertThat(carService.findBySellerId(buyer.getId())).isEmpty();
        assertThat(requestService.findByUserId(seller.getId())).extracting(PurchaseRequest::getCarId)
                .containsExactly(bmw.getId());
        assertThat(requestService.findByCarId(toyota.getId())).extracting(PurchaseRequest::getStatus)
                .containsExactly(RequestStatus.NEW, RequestStatus.APPROVED);
    }

    @Test
    void relatedLists_shouldNotDependOnMainTableSearch() {
        assertThat(requestService.search("Toyota")).hasSize(2);
        assertThat(requestService.findByUserId(seller.getId())).extracting(PurchaseRequest::getCarBrand)
                .containsExactly("BMW");
        assertThat(carService.search("Toyota")).hasSize(1);
        assertThat(carService.findBySellerId(other.getId())).extracting(Car::getBrand).containsExactly("BMW");
    }

    @Test
    void requestDetails_shouldReportDeletedRequestUserOrCar() {
        assertThatThrownBy(() -> requestService.getDetails(999L)).isInstanceOf(EntityNotFoundException.class);
        users.delete(buyer.getId());
        assertThatThrownBy(() -> requestService.getDetails(buyerRequest.getId()))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("Пользователь");
        users.seed(buyer);
        cars.delete(toyota.getId());
        assertThatThrownBy(() -> requestService.getDetails(buyerRequest.getId()))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("Автомобиль");
    }

    @Test
    void relatedLists_shouldReportMissingParentAndReturnEmptyWhenNoRelations() {
        assertThatThrownBy(() -> carService.findBySellerId(999L)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> requestService.findByUserId(999L)).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> requestService.findByCarId(999L)).isInstanceOf(EntityNotFoundException.class);
        User newcomer = new User("Новый", "new", "password", UserRole.USER);
        users.seed(newcomer);
        Car noRequests = seedCar(newcomer, "Kia", "Rio");
        assertThat(requestService.findByUserId(newcomer.getId())).isEmpty();
        assertThat(requestService.findByCarId(noRequests.getId())).isEmpty();
    }

    private Car seedCar(User owner, String brand, String model) {
        Car car = new Car(owner.getId(), brand, model, 2020, 45000, new BigDecimal("850000.00"),
                "JTNB11HK" + String.format("%09d", 3_000_001L + cars.findAll().size()),
                "Седан", "Автомат", "Бензин", new BigDecimal("2.5"),
                "Описание автомобиля", CarStatus.AVAILABLE);
        car.setSellerFullName(owner.getFullName());
        cars.seed(car);
        return car;
    }

    private PurchaseRequest seedRequest(User user, Car car, RequestStatus status) {
        PurchaseRequest request = new PurchaseRequest(user.getId(), car.getId(), "Хочу осмотреть автомобиль", status);
        request.setUserFullName(user.getFullName());
        request.setCarBrand(car.getBrand());
        request.setCarModel(car.getModel());
        request.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        request.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 12, 30));
        requests.seed(request);
        return request;
    }
}
