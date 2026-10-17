package carmarket.controller;

import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static carmarket.util.TableHelper.column;
import static carmarket.util.TableHelper.dateColumn;

public class RelatedDataController {
    private final ObservableList<Car> cars = FXCollections.observableArrayList();
    private final ObservableList<PurchaseRequest> requests = FXCollections.observableArrayList();

    @FXML private Label contextLabel;
    @FXML private TabPane relatedTabs;
    @FXML private Tab carsTab;
    @FXML private Tab requestsTab;
    @FXML private TableView<Car> relatedCarsTable;
    @FXML private TableColumn<Car, Long> carIdColumn;
    @FXML private TableColumn<Car, String> carNameColumn;
    @FXML private TableColumn<Car, Integer> yearColumn;
    @FXML private TableColumn<Car, Integer> mileageColumn;
    @FXML private TableColumn<Car, BigDecimal> priceColumn;
    @FXML private TableColumn<Car, String> carStatusColumn;
    @FXML private TableColumn<Car, String> vinColumn;
    @FXML private TableView<PurchaseRequest> relatedRequestsTable;
    @FXML private TableColumn<PurchaseRequest, Long> requestIdColumn;
    @FXML private TableColumn<PurchaseRequest, String> userColumn;
    @FXML private TableColumn<PurchaseRequest, String> requestCarColumn;
    @FXML private TableColumn<PurchaseRequest, String> statusColumn;
    @FXML private TableColumn<PurchaseRequest, String> messageColumn;
    @FXML private TableColumn<PurchaseRequest, LocalDateTime> createdAtColumn;

    @FXML
    private void initialize() {
        column(carIdColumn, Car::getId);
        column(carNameColumn, car -> car.getBrand() + " " + car.getModel());
        column(yearColumn, Car::getYear);
        column(mileageColumn, Car::getMileage);
        column(priceColumn, Car::getPrice);
        column(carStatusColumn, car -> car.getStatus().getDisplayName());
        column(vinColumn, Car::getVin);
        column(requestIdColumn, PurchaseRequest::getId);
        column(userColumn, PurchaseRequest::getUserFullName);
        column(requestCarColumn, request -> request.getCarBrand() + " " + request.getCarModel());
        column(statusColumn, request -> request.getStatus().getDisplayName());
        column(messageColumn, PurchaseRequest::getMessage);
        dateColumn(createdAtColumn, PurchaseRequest::getCreatedAt);
        relatedCarsTable.setItems(cars);
        relatedRequestsTable.setItems(requests);
    }

    public void setupForCar(Car car, List<PurchaseRequest> carRequests) {
        contextLabel.setText("Автомобиль №" + car.getId() + ": " + car.getBrand() + " " + car.getModel());
        setRows(List.of(), carRequests);
        relatedTabs.getTabs().setAll(requestsTab);
    }

    public void setupForUser(User user, List<Car> userCars, List<PurchaseRequest> userRequests) {
        contextLabel.setText("Пользователь: " + user.getFullName() + " (" + user.getLogin() + ")");
        setRows(userCars, userRequests);
        relatedTabs.getTabs().setAll(carsTab, requestsTab);
    }

    private void setRows(List<Car> newCars, List<PurchaseRequest> newRequests) {
        cars.setAll(newCars);
        requests.setAll(newRequests);
        carsTab.setText("Автомобили (" + cars.size() + ")");
        requestsTab.setText("Заявки (" + requests.size() + ")");
    }

    @FXML
    private void onClose() {
        ((Stage) contextLabel.getScene().getWindow()).close();
    }
}
