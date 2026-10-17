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
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void calculate_shouldCountEveryStatusAndPriceAndMileageIndicators() {
        List<Car> cars = List.of(car(CarStatus.AVAILABLE, "100000", 0),
                car(CarStatus.RESERVED, "200000", 100), car(CarStatus.SOLD, "300000", 200),
                car(CarStatus.ARCHIVED, "400000", 300));
        List<PurchaseRequest> requests = new java.util.ArrayList<>();
        int count = 1;
        for (RequestStatus status : RequestStatus.values()) {
            for (int i = 0; i < count; i++) {
                requests.add(request(status));
            }
            count++;
        }
        SystemStatistics statistics = new StatisticsService().calculate(List.of(), cars, requests);
        assertThat(statistics.averageCarPrice()).isEqualByComparingTo("250000.00");
        assertThat(statistics.averageMileage()).isEqualByComparingTo("150.00");
        assertThat(statistics.minCarPrice()).isEqualByComparingTo("100000");
        assertThat(statistics.maxCarPrice()).isEqualByComparingTo("400000");
        assertThat(statistics.approvedRequestPercentage()).isEqualByComparingTo("20.00");
        assertThat(statistics.activeRequests()).isEqualTo(3);
        assertThat(statistics.requestsByStatus()).containsEntry(RequestStatus.NEW, 1L)
                .containsEntry(RequestStatus.IN_PROGRESS, 2L).containsEntry(RequestStatus.APPROVED, 3L)
                .containsEntry(RequestStatus.REJECTED, 4L).containsEntry(RequestStatus.CANCELLED, 5L);
        assertThat(statistics.carsByStatus().values()).containsOnly(1L);
        assertThat(statistics.requestsByStatus().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(statistics.totalRequests());
    }

    @Test
    void calculate_shouldReturnZerosAndMissingAveragesForEmptyDatabase() {
        SystemStatistics statistics = new StatisticsService().calculate(List.of(), List.of(), List.of());
        assertThat(statistics.totalUsers()).isZero();
        assertThat(statistics.totalCars()).isZero();
        assertThat(statistics.totalRequests()).isZero();
        assertThat(statistics.requestsByStatus()).hasSize(RequestStatus.values().length);
        assertThat(statistics.requestsByStatus().values()).containsOnly(0L);
        assertThat(statistics.carsByStatus()).hasSize(CarStatus.values().length);
        assertThat(statistics.carsByStatus().values()).containsOnly(0L);
        assertThat(statistics.averageCarPrice()).isNull();
        assertThat(statistics.averageMileage()).isNull();
        assertThat(statistics.minCarPrice()).isNull();
        assertThat(statistics.maxCarPrice()).isNull();
        assertThat(statistics.approvedRequestPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_shouldRoundAveragesAndPercentageToTwoDecimalPlaces() {
        SystemStatistics statistics = new StatisticsService().calculate(List.of(),
                List.of(car(CarStatus.AVAILABLE, "0.10", 1), car(CarStatus.AVAILABLE, "0.20", 2),
                        car(CarStatus.AVAILABLE, "0.20", 2)),
                List.of(request(RequestStatus.APPROVED), request(RequestStatus.NEW), request(RequestStatus.REJECTED)));
        assertThat(statistics.averageCarPrice()).isEqualByComparingTo("0.17");
        assertThat(statistics.averageMileage()).isEqualByComparingTo("1.67");
        assertThat(statistics.approvedRequestPercentage()).isEqualByComparingTo("33.33");
    }

    @Test
    void load_shouldReadFreshRepositoryDataAfterChanges() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryCarRepository cars = new InMemoryCarRepository();
        InMemoryPurchaseRequestRepository requests = new InMemoryPurchaseRequestRepository();
        StatisticsService service = new StatisticsService(users, cars, requests);
        users.seed(new User("Иван", "ivan", "password", UserRole.USER));
        Car car = car(CarStatus.AVAILABLE);
        cars.seed(car);
        requests.seed(request(RequestStatus.NEW));
        SystemStatistics before = service.load();
        requests.seed(request(RequestStatus.APPROVED));
        car.setPrice(new BigDecimal("900000"));
        SystemStatistics after = service.load();
        assertThat(before.totalRequests()).isEqualTo(1);
        assertThat(after.totalRequests()).isEqualTo(2);
        assertThat(after.completedRequests()).isEqualTo(1);
        assertThat(after.approvedRequestPercentage()).isEqualByComparingTo("50.00");
        assertThat(before.averageCarPrice()).isEqualByComparingTo("1000000");
        assertThat(after.averageCarPrice()).isEqualByComparingTo("900000");
    }

    private Car car(CarStatus status, String price, int mileage) {
        Car car = car(status);
        car.setPrice(new BigDecimal(price));
        car.setMileage(mileage);
        return car;
    }

    private PurchaseRequest request(RequestStatus status) {
        return new PurchaseRequest(1L, 1L, "Сообщение", status);
    }
}
