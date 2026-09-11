package carmarket.repository;

import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.util.DatabaseManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RepositoryTest {

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
    void findAllUsers_shouldReturnUsersFromDatabase() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        UserRepository repository = new UserRepository();
        List<User> users = repository.findAll();

        assertFalse(users.isEmpty());

        User firstUser = users.get(0);
        assertNotNull(firstUser.getId());
        assertNotNull(firstUser.getLogin());
        assertNotNull(firstUser.getFullName());
        assertNotNull(firstUser.getRole());
    }

    @Test
    void findUserById_shouldReturnSameUser() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        UserRepository repository = new UserRepository();
        User fromList = repository.findAll().get(0);
        User fromId = repository.findById(fromList.getId());

        assertNotNull(fromId);
        assertEquals(fromList.getId(), fromId.getId());
        assertEquals(fromList.getLogin(), fromId.getLogin());
        assertEquals(fromList.getFullName(), fromId.getFullName());
    }

    @Test
    void findUserById_shouldReturnNullWhenNotFound() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        UserRepository repository = new UserRepository();
        User user = repository.findById(9999L);

        assertNull(user);
    }

    @Test
    void findAllCars_shouldReturnCarsFromDatabase() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        CarRepository repository = new CarRepository();
        List<Car> cars = repository.findAll();

        assertFalse(cars.isEmpty());

        Car firstCar = cars.get(0);
        assertNotNull(firstCar.getId());
        assertNotNull(firstCar.getBrand());
        assertNotNull(firstCar.getModel());
        assertNotNull(firstCar.getStatus());
    }

    @Test
    void findCarById_shouldReturnSameCar() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        CarRepository repository = new CarRepository();
        Car fromList = repository.findAll().get(0);
        Car fromId = repository.findById(fromList.getId());

        assertNotNull(fromId);
        assertEquals(fromList.getId(), fromId.getId());
        assertEquals(fromList.getBrand(), fromId.getBrand());
        assertEquals(fromList.getModel(), fromId.getModel());
    }

    @Test
    void findAllPurchaseRequests_shouldReturnRequestsFromDatabase() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        PurchaseRequestRepository repository = new PurchaseRequestRepository();
        List<PurchaseRequest> requests = repository.findAll();

        assertFalse(requests.isEmpty());

        PurchaseRequest firstRequest = requests.get(0);
        assertNotNull(firstRequest.getId());
        assertNotNull(firstRequest.getUserId());
        assertNotNull(firstRequest.getCarId());
        assertNotNull(firstRequest.getStatus());
    }

    @Test
    void findPurchaseRequestById_shouldReturnSameRequest() {
        assumeTrue(databaseAvailable, "PostgreSQL недоступен — проверь database.properties");

        PurchaseRequestRepository repository = new PurchaseRequestRepository();
        PurchaseRequest fromList = repository.findAll().get(0);
        PurchaseRequest fromId = repository.findById(fromList.getId());

        assertNotNull(fromId);
        assertEquals(fromList.getId(), fromId.getId());
        assertEquals(fromList.getUserId(), fromId.getUserId());
        assertEquals(fromList.getCarId(), fromId.getCarId());
    }
}
