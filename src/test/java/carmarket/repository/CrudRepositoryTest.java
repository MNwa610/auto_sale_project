package carmarket.repository;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.UserRole;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.util.DatabaseManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CrudRepositoryTest {

    private static boolean databaseAvailable;

    @BeforeAll
    static void checkDatabaseConnection() {
        try (Connection connection = DatabaseManager.getConnection()) {
            databaseAvailable = connection.isValid(2);
        } catch (SQLException e) {
            databaseAvailable = false;
        }
    }

    @Test
    void userCrud_shouldWork() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        UserRepository repository = new UserRepository();
        String login = "test_user_" + System.currentTimeMillis();

        User user = new User("Тестовый Пользователь", login, "hash_test", UserRole.USER);
        User created = repository.create(user);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());

        User found = repository.findById(created.getId());
        assertEquals(login, found.getLogin());

        found.setFullName("Обновлённое Имя");
        assertTrue(repository.update(found));

        User updated = repository.findById(created.getId());
        assertEquals("Обновлённое Имя", updated.getFullName());

        assertTrue(repository.delete(created.getId()));
    }

    @Test
    void carCrud_shouldWork() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        UserRepository userRepository = new UserRepository();
        CarRepository carRepository = new CarRepository();

        User seller = userRepository.findAll().get(0);

        Car car = new Car(
                seller.getId(),
                "TestBrand",
                "TestModel",
                2020,
                50000,
                new BigDecimal("1000000.00"),
                null,
                "sedan",
                "automatic",
                "petrol",
                new BigDecimal("2.0"),
                "Тестовый автомобиль",
                CarStatus.AVAILABLE
        );

        Car created = carRepository.create(car);
        assertNotNull(created.getId());

        Car found = carRepository.findById(created.getId());
        assertEquals("TestBrand", found.getBrand());

        found.setMileage(55000);
        assertTrue(carRepository.update(found));

        Car updated = carRepository.findById(created.getId());
        assertEquals(55000, updated.getMileage());

        assertTrue(carRepository.delete(created.getId()));
    }

    @Test
    void purchaseRequestCrud_shouldWork() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        UserRepository userRepository = new UserRepository();
        CarRepository carRepository = new CarRepository();
        PurchaseRequestRepository requestRepository = new PurchaseRequestRepository();

        User user = userRepository.findAll().get(0);
        Car car = carRepository.findAll().get(0);

        PurchaseRequest request = new PurchaseRequest(
                user.getId(),
                car.getId(),
                "Тестовая заявка",
                RequestStatus.NEW
        );

        PurchaseRequest created = requestRepository.create(request);
        assertNotNull(created.getId());

        PurchaseRequest found = requestRepository.findById(created.getId());
        assertEquals("Тестовая заявка", found.getMessage());

        found.setStatus(RequestStatus.IN_PROGRESS);
        assertTrue(requestRepository.update(found));

        PurchaseRequest updated = requestRepository.findById(created.getId());
        assertEquals(RequestStatus.IN_PROGRESS, updated.getStatus());

        assertTrue(requestRepository.delete(created.getId()));
    }
}
