package carmarket.controller;

import carmarket.enums.RequestStatus;
import carmarket.exception.DatabaseException;
import carmarket.model.PurchaseRequest;
import carmarket.service.PurchaseRequestService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final PurchaseRequestService purchaseRequestService = new PurchaseRequestService();
    private final ObservableList<PurchaseRequest> requests = FXCollections.observableArrayList();

    @FXML
    private TableView<PurchaseRequest> requestsTable;
    @FXML
    private TableColumn<PurchaseRequest, Long> idColumn;
    @FXML
    private TableColumn<PurchaseRequest, String> userColumn;
    @FXML
    private TableColumn<PurchaseRequest, String> carColumn;
    @FXML
    private TableColumn<PurchaseRequest, String> statusColumn;
    @FXML
    private TableColumn<PurchaseRequest, String> messageColumn;
    @FXML
    private TableColumn<PurchaseRequest, String> createdAtColumn;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getId()));
        userColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(text(cell.getValue().getUserFullName())));
        carColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatCar(cell.getValue())));
        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatStatus(cell.getValue().getStatus())));
        messageColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(text(cell.getValue().getMessage())));
        createdAtColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatDate(cell.getValue())));

        requestsTable.setItems(requests);
        loadRequests();
    }

    @FXML
    private void onRefresh() {
        loadRequests();
    }

    private void loadRequests() {
        try {
            requests.setAll(purchaseRequestService.findAllWithDetails());
        } catch (DatabaseException e) {
            log.error("Не удалось загрузить заявки", e);
            requests.clear();
        }
    }

    private static String formatCar(PurchaseRequest request) {
        String brand = text(request.getCarBrand());
        String model = text(request.getCarModel());
        return (brand + " " + model).trim();
    }

    private static String formatStatus(RequestStatus status) {
        if (status == null) {
            return "";
        }
        return status.getDisplayName();
    }

    private static String formatDate(PurchaseRequest request) {
        if (request.getCreatedAt() == null) {
            return "";
        }
        return request.getCreatedAt().format(DATE_FORMAT);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
