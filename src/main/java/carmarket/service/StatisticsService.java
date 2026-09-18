package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;

import java.util.List;

public class StatisticsService {

        public SystemStatistics calculate(List<User> users,
                                      List<Car> cars,
                                      List<PurchaseRequest> requests) {
        long activeRequests = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.NEW
                        || request.getStatus() == RequestStatus.IN_PROGRESS)
                .count();

        long availableCars = cars.stream()
                .filter(car -> car.getStatus() == CarStatus.AVAILABLE)
                .count();

        long soldCars = cars.stream()
                .filter(car -> car.getStatus() == CarStatus.SOLD)
                .count();

        long completedRequests = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.APPROVED)
                .count();

        long cancelledRequests = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.CANCELLED)
                .count();

        return new SystemStatistics(
                users.size(),
                cars.size(),
                availableCars,
                soldCars,
                requests.size(),
                activeRequests,
                completedRequests,
                cancelledRequests
        );
    }
}