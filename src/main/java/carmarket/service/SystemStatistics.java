package carmarket.service;

public record SystemStatistics(
        long totalUsers,
        long totalCars,
        long availableCars,
        long soldCars,
        long totalRequests,
        long activeRequests,
        long completedRequests,
        long cancelledRequests
) {
}