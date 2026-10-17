package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;

import java.math.BigDecimal;
import java.util.Map;

public record SystemStatistics(
        long totalUsers,
        long totalCars,
        long availableCars,
        long soldCars,
        long totalRequests,
        long activeRequests,
        long completedRequests,
        long cancelledRequests,
        Map<RequestStatus, Long> requestsByStatus,
        Map<CarStatus, Long> carsByStatus,
        BigDecimal averageCarPrice,
        BigDecimal averageMileage,
        BigDecimal minCarPrice,
        BigDecimal maxCarPrice,
        BigDecimal approvedRequestPercentage
) {
    public SystemStatistics {
        requestsByStatus = Map.copyOf(requestsByStatus);
        carsByStatus = Map.copyOf(carsByStatus);
    }
}
