package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseRequestServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryCarRepository carRepository;
    private InMemoryPurchaseRequestRepository requestRepository;
    private PurchaseRequestService requestService;

    private Long userId;
    private Long availableCarId;
    private Long soldCarId;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        carRepository = new InMemoryCarRepository();
        requestRepository = new InMemoryPurchaseRequestRepository();
        requestService = new PurchaseRequestService(requestRepository, userRepository, carRepository);

        User user = new User("Покупатель", "buyer1", "hash", UserRole.USER);
        userRepository.seed(user);
        userId = user.getId();

        Car availableCar = new Car(
                userId, "Kia", "Rio", 2018, 70000,
                new BigDecimal("650000"), null, "hatchback", "manual", "petrol",
                new BigDecimal("1.6"), null, CarStatus.AVAILABLE
        );
        carRepository.seed(availableCar);
        availableCarId = availableCar.getId();

        Car soldCar = new Car(
                userId, "BMW", "X5", 2015, 120000,
                new BigDecimal("2100000"), null, "suv", "automatic", "diesel",
                new BigDecimal("3.0"), null, CarStatus.SOLD
        );
        carRepository.seed(soldCar);
        soldCarId = soldCar.getId();
    }

    @Test
    void create_shouldFailWhenUserNotFound() {
        PurchaseRequest request = new PurchaseRequest(999L, availableCarId, "Хочу купить", RequestStatus.NEW);

        assertThrows(EntityNotFoundException.class, () -> requestService.create(request));
    }

    @Test
    void create_shouldFailWhenCarNotFound() {
        PurchaseRequest request = new PurchaseRequest(userId, 999L, "Хочу купить", RequestStatus.NEW);

        assertThrows(EntityNotFoundException.class, () -> requestService.create(request));
    }

    @Test
    void create_shouldFailWhenCarIsSold() {
        PurchaseRequest request = new PurchaseRequest(userId, soldCarId, "Хочу купить", RequestStatus.NEW);

        BusinessException exception = assertThrows(BusinessException.class, () -> requestService.create(request));
        assertEquals("Нельзя создать заявку на автомобиль со статусом SOLD", exception.getMessage());
    }

    @Test
    void create_shouldFailWhenCarIsArchived() {
        Car archivedCar = new Car(
                userId, "Renault", "Duster", 2017, 90000,
                new BigDecimal("900000"), null, "suv", "manual", "petrol",
                new BigDecimal("1.6"), null, CarStatus.ARCHIVED
        );
        carRepository.seed(archivedCar);

        PurchaseRequest request = new PurchaseRequest(userId, archivedCar.getId(), "Хочу купить", RequestStatus.NEW);

        BusinessException exception = assertThrows(BusinessException.class, () -> requestService.create(request));
        assertEquals("Нельзя создать заявку на автомобиль со статусом ARCHIVED", exception.getMessage());
    }

    @Test
    void create_shouldFailWhenActiveRequestAlreadyExists() {
        PurchaseRequest first = new PurchaseRequest(userId, availableCarId, "Первая заявка", RequestStatus.NEW);
        requestService.create(first);

        PurchaseRequest second = new PurchaseRequest(userId, availableCarId, "Вторая заявка", RequestStatus.NEW);

        BusinessException exception = assertThrows(BusinessException.class, () -> requestService.create(second));
        assertEquals("У пользователя уже есть активная заявка на этот автомобиль", exception.getMessage());
    }

    @Test
    void create_shouldSucceedForValidRequest() {
        PurchaseRequest request = new PurchaseRequest(userId, availableCarId, "Заявка", RequestStatus.NEW);

        PurchaseRequest created = requestService.create(request);

        assertNotNull(created.getId());
        assertEquals(RequestStatus.NEW, created.getStatus());
    }

    @Test
    void update_shouldAllowNewToInProgress() {
        PurchaseRequest created = requestService.create(
                new PurchaseRequest(userId, availableCarId, "Заявка", RequestStatus.NEW)
        );
        PurchaseRequest forUpdate = copyRequest(created);
        forUpdate.setStatus(RequestStatus.IN_PROGRESS);

        PurchaseRequest updated = requestService.update(forUpdate);

        assertEquals(RequestStatus.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void update_shouldRejectInvalidStatusTransition() {
        PurchaseRequest created = requestService.create(
                new PurchaseRequest(userId, availableCarId, "Заявка", RequestStatus.NEW)
        );

        PurchaseRequest forUpdate = copyRequest(created);
        forUpdate.setStatus(RequestStatus.APPROVED);

        BusinessException exception = assertThrows(BusinessException.class, () -> requestService.update(forUpdate));
        assertEquals("Недопустимый переход статуса: NEW -> APPROVED", exception.getMessage());
    }

    @Test
    void update_shouldRejectTransitionFromFinalStatus() {
        PurchaseRequest stored = new PurchaseRequest(userId, availableCarId, "Заявка", RequestStatus.APPROVED);
        requestRepository.seed(stored);

        PurchaseRequest forUpdate = copyRequest(stored);
        forUpdate.setStatus(RequestStatus.CANCELLED);

        BusinessException exception = assertThrows(BusinessException.class, () -> requestService.update(forUpdate));
        assertEquals("Недопустимый переход статуса: APPROVED -> CANCELLED", exception.getMessage());
    }

    private PurchaseRequest copyRequest(PurchaseRequest source) {
        PurchaseRequest copy = new PurchaseRequest(
                source.getUserId(),
                source.getCarId(),
                source.getMessage(),
                source.getStatus()
        );
        copy.setId(source.getId());
        return copy;
    }
}
