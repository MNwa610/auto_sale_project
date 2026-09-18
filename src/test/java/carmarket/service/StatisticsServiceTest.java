package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.UserRole;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsServiceTest {

    @Test
    void calculate_shouldCountSystemIndicators() {
        User user = new User("Иван Иванов", "ivan", "hash", UserRole.USER);
        Car availableCar = car(CarStatus.AVAILABLE);
        Car soldCar = car(CarStatus.SOLD);
        List<PurchaseRequest> requests = List.of(
                request(RequestStatus.NEW),
                request(RequestStatus.IN_PROGRESS),
                request(RequestStatus.APPROVED),
                request(RequestStatus.CANCELLED)
        );

        SystemStatistics statistics = new StatisticsService().calculate(
                List.of(user),
                List.of(availableCar, soldCar),
                requests
        );

        assertEquals(1, statistics.totalUsers());
        assertEquals(2, statistics.totalCars());
        assertEquals(1, statistics.availableCars());
        assertEquals(1, statistics.soldCars());
        assertEquals(4, statistics.totalRequests());
        assertEquals(2, statistics.activeRequests());
        assertEquals(1, statistics.completedRequests());
        assertEquals(1, statistics.cancelledRequests());
    }

    private Car car(CarStatus status) {
        return new Car(1L, "Toyota", "Camry", 2020, 10000,
                new BigDecimal("1000000"), "VIN", "sedan", "automatic",
                "petrol", new BigDecimal("2.5"), "Описание", status);
    }

    private PurchaseRequest request(RequestStatus status) {
        return new PurchaseRequest(1L, 1L, "Сообщение", status);
    }
}