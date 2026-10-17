package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.repository.CarRepository;
import carmarket.repository.PurchaseRequestRepository;
import carmarket.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final PurchaseRequestRepository requestRepository;

    public StatisticsService() {
        this(new UserRepository(), new CarRepository(), new PurchaseRequestRepository());
    }

    public StatisticsService(UserRepository userRepository, CarRepository carRepository,
                             PurchaseRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.carRepository = carRepository;
        this.requestRepository = requestRepository;
    }

    public SystemStatistics load() {
        return calculate(userRepository.findAll(), carRepository.findAll(), requestRepository.findAll());
    }

    public SystemStatistics calculate(List<User> users, List<Car> cars, List<PurchaseRequest> requests) {
        Map<CarStatus, Long> carsByStatus = new EnumMap<>(CarStatus.class);
        for (CarStatus status : CarStatus.values()) {
            carsByStatus.put(status, 0L);
        }
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalMileage = BigDecimal.ZERO;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        for (Car car : cars) {
            carsByStatus.merge(car.getStatus(), 1L, Long::sum);
            totalPrice = totalPrice.add(car.getPrice());
            totalMileage = totalMileage.add(BigDecimal.valueOf(car.getMileage()));
            minPrice = minPrice == null ? car.getPrice() : minPrice.min(car.getPrice());
            maxPrice = maxPrice == null ? car.getPrice() : maxPrice.max(car.getPrice());
        }

        Map<RequestStatus, Long> requestsByStatus = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            requestsByStatus.put(status, 0L);
        }
        for (PurchaseRequest request : requests) {
            requestsByStatus.merge(request.getStatus(), 1L, Long::sum);
        }
        long approved = requestsByStatus.get(RequestStatus.APPROVED);
        BigDecimal approvedPercentage = requests.isEmpty() ? new BigDecimal("0.00")
                : BigDecimal.valueOf(approved).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requests.size()), 2, RoundingMode.HALF_UP);

        return new SystemStatistics(users.size(), cars.size(), carsByStatus.get(CarStatus.AVAILABLE),
                carsByStatus.get(CarStatus.SOLD), requests.size(),
                requestsByStatus.get(RequestStatus.NEW) + requestsByStatus.get(RequestStatus.IN_PROGRESS),
                approved, requestsByStatus.get(RequestStatus.CANCELLED), requestsByStatus, carsByStatus,
                average(totalPrice, cars.size()), average(totalMileage, cars.size()),
                minPrice, maxPrice, approvedPercentage);
    }

    private BigDecimal average(BigDecimal total, int count) {
        return count == 0 ? null : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
