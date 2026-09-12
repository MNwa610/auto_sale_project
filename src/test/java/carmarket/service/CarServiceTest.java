package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.Car;
import carmarket.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryCarRepository carRepository;
    private CarService carService;
    private Long sellerId;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        carRepository = new InMemoryCarRepository();
        carService = new CarService(carRepository, userRepository);

        User seller = new User("Иван Иванов", "seller1", "hash", UserRole.USER);
        userRepository.seed(seller);
        sellerId = seller.getId();
    }

    @Test
    void create_shouldFailWhenPriceIsZeroOrNegative() {
        Car car = sampleCar(sellerId, BigDecimal.ZERO, 10000);

        BusinessException exception = assertThrows(BusinessException.class, () -> carService.create(car));
        assertEquals("Цена автомобиля должна быть больше 0", exception.getMessage());
    }

    @Test
    void create_shouldFailWhenMileageIsNegative() {
        Car car = sampleCar(sellerId, new BigDecimal("500000"), -1);

        BusinessException exception = assertThrows(BusinessException.class, () -> carService.create(car));
        assertEquals("Пробег не может быть отрицательным", exception.getMessage());
    }

    @Test
    void create_shouldFailWhenSellerNotFound() {
        Car car = sampleCar(999L, new BigDecimal("500000"), 10000);

        assertThrows(EntityNotFoundException.class, () -> carService.create(car));
    }

    @Test
    void create_shouldSucceedWhenDataIsValid() {
        Car car = sampleCar(sellerId, new BigDecimal("850000.00"), 45000);

        Car created = carService.create(car);

        assertNotNull(created.getId());
        assertEquals("Toyota", created.getBrand());
    }

    private Car sampleCar(Long sellerId, BigDecimal price, int mileage) {
        return new Car(
                sellerId,
                "Toyota",
                "Camry",
                2019,
                mileage,
                price,
                null,
                "sedan",
                "automatic",
                "petrol",
                new BigDecimal("2.5"),
                "Описание",
                CarStatus.AVAILABLE
        );
    }
}
