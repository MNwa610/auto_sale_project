package carmarket.controller;

import carmarket.enums.RequestStatus;
import carmarket.exception.BusinessException;
import carmarket.exception.DatabaseException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.service.CarService;
import carmarket.service.PurchaseRequestService;
import carmarket.util.AlertHelper;
import carmarket.util.DialogHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class MainController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final PurchaseRequestService requestService = new PurchaseRequestService();
    private final CarService carService = new CarService();
    private final ObservableList<PurchaseRequest> requests = FXCollections.observableArrayList();
    private final ObservableList<Car> cars = FXCollections.observableArrayList();

    @FXML private TableView<PurchaseRequest> requestsTable;
    @FXML private TableColumn<PurchaseRequest, Long> idColumn;
    @FXML private TableColumn<PurchaseRequest, String> userColumn;
    @FXML private TableColumn<PurchaseRequest, String> carColumn;
    @FXML private TableColumn<PurchaseRequest, String> statusColumn;
    @FXML private TableColumn<PurchaseRequest, String> messageColumn;
    @FXML private TableColumn<PurchaseRequest, LocalDateTime> createdAtColumn;
    @FXML private TableView<Car> carsTable;
    @FXML private TableColumn<Car, String> brandColumn;
    @FXML private TableColumn<Car, String> modelColumn;
    @FXML private TableColumn<Car, Integer> yearColumn;
    @FXML private TableColumn<Car, Integer> mileageColumn;
    @FXML private TableColumn<Car, BigDecimal> priceColumn;
    @FXML private TableColumn<Car, String> carStatusColumn;
    @FXML private TableColumn<Car, String> sellerColumn;
    @FXML private TableColumn<Car, String> vinColumn;

    @FXML
    private void initialize() {
        column(idColumn, PurchaseRequest::getId);
        column(userColumn, PurchaseRequest::getUserFullName);
        column(carColumn, r -> r.getCarBrand() + " " + r.getCarModel());
        column(statusColumn, r -> r.getStatus().getDisplayName());
        column(messageColumn, PurchaseRequest::getMessage);
        column(createdAtColumn, PurchaseRequest::getCreatedAt);
        createdAtColumn.setCellFactory(ignored -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FORMAT));
            }
        });
        column(brandColumn, Car::getBrand);
        column(modelColumn, Car::getModel);
        column(yearColumn, Car::getYear);
        column(mileageColumn, Car::getMileage);
        column(priceColumn, Car::getPrice);
        column(carStatusColumn, c -> c.getStatus().getDisplayName());
        column(sellerColumn, Car::getSellerFullName);
        column(vinColumn, Car::getVin);
        requestsTable.setItems(requests);
        carsTable.setItems(cars);
    }

    private <T, V> void column(TableColumn<T, V> column, Function<T, V> value) {
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(value.apply(cell.getValue())));
    }

    public void refreshAll() {
        // Ошибка БД не должна очищать уже показанные данные.
        List<PurchaseRequest> freshRequests = requestService.findAllWithDetails();
        List<Car> freshCars = carService.findAll();
        requests.setAll(freshRequests);
        cars.setAll(freshCars);
        requestsTable.sort();
        carsTable.sort();
    }

    @FXML private void onRefresh() {
        perform(this::refreshAll);
    }

    @FXML private void onAddRequest() {
        perform(() -> openRequest(null));
    }

    @FXML private void onEditRequest() {
        PurchaseRequest selected = selected(requestsTable, "Выберите заявку для изменения.");
        if (selected != null) {
            perform(() -> openRequest(requestService.findById(selected.getId())));
        }
    }

    private void openRequest(PurchaseRequest request) {
        RequestDialogController dialog = DialogHelper.show("/fxml/request-dialog.fxml",
                request == null ? "Добавление заявки" : "Редактирование заявки", owner(),
                (RequestDialogController controller) -> controller.setup(request));
        if (dialog.getSavedId() != null) {
            reloadRequests(dialog.getSavedId());
        }
    }

    @FXML private void onDeleteRequest() {
        PurchaseRequest request = selected(requestsTable, "Выберите заявку для удаления.");
        if (request != null && AlertHelper.confirm(owner(), "Удалить заявку №" + request.getId()
                + " — " + request.getCarBrand() + " " + request.getCarModel() + "?")) {
            perform(() -> {
                requestService.delete(request.getId());
                reloadRequests(null);
            });
        }
    }

    @FXML private void onChangeStatus() {
        PurchaseRequest selected = selected(requestsTable, "Выберите заявку для смены статуса.");
        if (selected == null) {
            return;
        }
        perform(() -> {
            PurchaseRequest request = requestService.findById(selected.getId());
            List<RequestStatus> allowed = requestService.allowedStatuses(request.getStatus());
            if (allowed.isEmpty()) {
                AlertHelper.information(owner(), "Заявка завершена. Дальнейшая смена статуса недоступна.");
                return;
            }
            AlertHelper.choose(owner(), "Статус заявки №" + request.getId(), allowed, RequestStatus::getDisplayName)
                    .ifPresent(status -> {
                        requestService.changeStatus(request.getId(), status);
                        reloadRequests(request.getId());
                        if (status == RequestStatus.APPROVED) {
                            AlertHelper.information(owner(), "Заявка одобрена.");
                        }
                    });
        });
    }

    private void reloadRequests(Long id) {
        requests.setAll(requestService.findAllWithDetails());
        requestsTable.sort();
        selectById(requestsTable, id, PurchaseRequest::getId);
    }

    @FXML private void onAddCar() {
        perform(() -> openCar(null));
    }

    @FXML private void onEditCar() {
        Car selected = selected(carsTable, "Выберите автомобиль для изменения.");
        if (selected != null) {
            perform(() -> openCar(carService.findById(selected.getId())));
        }
    }

    private void openCar(Car car) {
        CarDialogController dialog = DialogHelper.show("/fxml/car-dialog.fxml",
                car == null ? "Добавление автомобиля" : "Редактирование автомобиля", owner(),
                (CarDialogController controller) -> controller.setup(car));
        if (dialog.getSavedId() != null) {
            refreshAll();
            selectById(carsTable, dialog.getSavedId(), Car::getId);
        }
    }

    @FXML private void onDeleteCar() {
        Car car = selected(carsTable, "Выберите автомобиль для удаления.");
        if (car != null && AlertHelper.confirm(owner(), "Удалить автомобиль «"
                + car.getBrand() + " " + car.getModel() + "», №" + car.getId() + "?")) {
            perform(() -> {
                carService.delete(car.getId());
                refreshAll();
            });
        }
    }

    private <T> T selected(TableView<T> table, String warning) {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.warning(owner(), warning);
        }
        return selected;
    }

    private <T> void selectById(TableView<T> table, Long id, Function<T, Long> key) {
        table.getItems().stream().filter(row -> key.apply(row).equals(id)).findFirst().ifPresent(row -> {
            table.getSelectionModel().select(row);
            table.scrollTo(row);
        });
    }

    private void perform(Runnable action) {
        try {
            action.run();
        } catch (BusinessException | DatabaseException | UncheckedIOException e) {
            AlertHelper.error(owner(), e);
        }
    }

    private Window owner() {
        return requestsTable.getScene().getWindow();
    }
}
