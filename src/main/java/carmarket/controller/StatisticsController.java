package carmarket.controller;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.exception.DatabaseException;
import carmarket.service.StatisticsService;
import carmarket.service.SystemStatistics;
import carmarket.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class StatisticsController {
    private final StatisticsService service;
    private Map<RequestStatus, Label> requestLabels;
    private Map<CarStatus, Label> carLabels;

    @FXML private Label totalUsersLabel;
    @FXML private Label totalCarsLabel;
    @FXML private Label totalRequestsLabel;
    @FXML private Label averagePriceLabel;
    @FXML private Label averageMileageLabel;
    @FXML private Label minPriceLabel;
    @FXML private Label maxPriceLabel;
    @FXML private Label approvedPercentageLabel;
    @FXML private Label newRequestsLabel;
    @FXML private Label inProgressRequestsLabel;
    @FXML private Label approvedRequestsLabel;
    @FXML private Label rejectedRequestsLabel;
    @FXML private Label cancelledRequestsLabel;
    @FXML private Label availableCarsLabel;
    @FXML private Label reservedCarsLabel;
    @FXML private Label soldCarsLabel;
    @FXML private Label archivedCarsLabel;

    public StatisticsController() {
        this(new StatisticsService());
    }

    public StatisticsController(StatisticsService service) {
        this.service = service;
    }

    @FXML
    private void initialize() {
        requestLabels = Map.of(RequestStatus.NEW, newRequestsLabel,
                RequestStatus.IN_PROGRESS, inProgressRequestsLabel, RequestStatus.APPROVED, approvedRequestsLabel,
                RequestStatus.REJECTED, rejectedRequestsLabel, RequestStatus.CANCELLED, cancelledRequestsLabel);
        carLabels = Map.of(CarStatus.AVAILABLE, availableCarsLabel, CarStatus.RESERVED, reservedCarsLabel,
                CarStatus.SOLD, soldCarsLabel, CarStatus.ARCHIVED, archivedCarsLabel);
    }

    @FXML
    public void refresh() {
        try {
            SystemStatistics statistics = service.load();
            totalUsersLabel.setText(Long.toString(statistics.totalUsers()));
            totalCarsLabel.setText(Long.toString(statistics.totalCars()));
            totalRequestsLabel.setText(Long.toString(statistics.totalRequests()));
            averagePriceLabel.setText(number(statistics.averageCarPrice(), " ₽"));
            averageMileageLabel.setText(number(statistics.averageMileage(), " км"));
            minPriceLabel.setText(number(statistics.minCarPrice(), " ₽"));
            maxPriceLabel.setText(number(statistics.maxCarPrice(), " ₽"));
            approvedPercentageLabel.setText(number(statistics.approvedRequestPercentage(), " %"));
            requestLabels.forEach((status, label) -> label.setText(Long.toString(statistics.requestsByStatus().get(status))));
            carLabels.forEach((status, label) -> label.setText(Long.toString(statistics.carsByStatus().get(status))));
        } catch (DatabaseException e) {
            AlertHelper.error(totalUsersLabel.getScene().getWindow(), e);
        }
    }

    private String number(BigDecimal value, String unit) {
        return value == null ? "—" : value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + unit;
    }
}
