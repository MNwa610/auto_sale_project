package carmarket.controller;

import carmarket.enums.RequestStatus;
import carmarket.enums.SortField;
import carmarket.exception.BusinessException;
import carmarket.exception.DatabaseException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.model.TableQuery;
import carmarket.model.RequestDetails;
import carmarket.service.CarService;
import carmarket.service.PurchaseRequestService;
import carmarket.service.UserService;
import carmarket.util.AlertHelper;
import carmarket.util.DialogHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static carmarket.util.TableHelper.column;
import static carmarket.util.TableHelper.dateColumn;

public class MainController {
    private final PurchaseRequestService requestService;
    private final CarService carService;
    private final UserService userService;
    private final ObservableList<PurchaseRequest> requests = FXCollections.observableArrayList();
    private final ObservableList<Car> cars = FXCollections.observableArrayList();
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private TableQuery requestQuery = TableQuery.empty();
    private TableQuery carQuery = TableQuery.empty();

    @FXML private TableView<PurchaseRequest> requestsTable;
    @FXML private TableColumn<PurchaseRequest, Long> idColumn;
    @FXML private TableColumn<PurchaseRequest, String> userColumn;
    @FXML private TableColumn<PurchaseRequest, String> carColumn;
    @FXML private TableColumn<PurchaseRequest, String> statusColumn;
    @FXML private TableColumn<PurchaseRequest, String> messageColumn;
    @FXML private TableColumn<PurchaseRequest, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<PurchaseRequest, BigDecimal> requestPriceColumn;
    @FXML private TableColumn<PurchaseRequest, Integer> requestMileageColumn;
    @FXML private TextField requestSearchField;
    @FXML private ComboBox<RequestStatus> requestStatusBox;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<SortField> requestSortBox;
    @FXML private ComboBox<String> requestDirectionBox;
    @FXML private TableView<Car> carsTable;
    @FXML private TableColumn<Car, String> brandColumn;
    @FXML private TableColumn<Car, String> modelColumn;
    @FXML private TableColumn<Car, Integer> yearColumn;
    @FXML private TableColumn<Car, Integer> mileageColumn;
    @FXML private TableColumn<Car, BigDecimal> priceColumn;
    @FXML private TableColumn<Car, String> carStatusColumn;
    @FXML private TableColumn<Car, String> sellerColumn;
    @FXML private TableColumn<Car, String> vinColumn;
    @FXML private TextField carSearchField;
    @FXML private ComboBox<SortField> carSortBox;
    @FXML private ComboBox<String> carDirectionBox;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> loginColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, LocalDateTime> registeredAtColumn;

    public MainController() {
        this(new PurchaseRequestService(), new CarService(), new UserService());
    }

    public MainController(PurchaseRequestService requestService, CarService carService, UserService userService) {
        this.requestService = requestService;
        this.carService = carService;
        this.userService = userService;
    }

    @FXML
    private void initialize() {
        column(idColumn, PurchaseRequest::getId);
        column(userColumn, PurchaseRequest::getUserFullName);
        column(carColumn, r -> r.getCarBrand() + " " + r.getCarModel());
        column(statusColumn, r -> r.getStatus().getDisplayName());
        column(messageColumn, PurchaseRequest::getMessage);
        dateColumn(createdAtColumn, PurchaseRequest::getCreatedAt);
        column(requestPriceColumn, PurchaseRequest::getCarPrice);
        column(requestMileageColumn, PurchaseRequest::getCarMileage);
        column(brandColumn, Car::getBrand);
        column(modelColumn, Car::getModel);
        column(yearColumn, Car::getYear);
        column(mileageColumn, Car::getMileage);
        column(priceColumn, Car::getPrice);
        column(carStatusColumn, c -> c.getStatus().getDisplayName());
        column(sellerColumn, Car::getSellerFullName);
        column(vinColumn, Car::getVin);
        column(fullNameColumn, User::getFullName);
        column(loginColumn, User::getLogin);
        column(roleColumn, user -> user.getRole().getDisplayName());
        dateColumn(registeredAtColumn, User::getCreatedAt);
        requestsTable.setItems(requests);
        carsTable.setItems(cars);
        usersTable.setItems(users);
        setupQueryControls();
    }

    private void setupQueryControls() {
        // null обозначает вариант «Все»; отдельного статуса в БД для него нет.
        requestStatusBox.getItems().add(null);
        requestStatusBox.getItems().addAll(RequestStatus.values());
        requestStatusBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(RequestStatus status) {
                return status == null ? "Все" : status.getDisplayName();
            }

            @Override public RequestStatus fromString(String value) {
                throw new UnsupportedOperationException("Выберите статус из списка");
            }
        });
        requestStatusBox.setPromptText("Все");
        requestStatusBox.setCellFactory(list -> requestStatusCell());
        requestStatusBox.setButtonCell(requestStatusCell());
        requestStatusBox.getSelectionModel().selectFirst();
        DialogHelper.display(requestSortBox, SortField::getDisplayName);
        DialogHelper.display(carSortBox, SortField::getDisplayName);
        requestSortBox.getItems().setAll(SortField.values());
        carSortBox.getItems().setAll(SortField.DEFAULT, SortField.PRICE, SortField.MILEAGE);
        requestSortBox.setValue(SortField.DEFAULT);
        carSortBox.setValue(SortField.DEFAULT);
        requestDirectionBox.getItems().setAll("По возрастанию", "По убыванию");
        carDirectionBox.getItems().setAll(requestDirectionBox.getItems());
        requestDirectionBox.getSelectionModel().selectFirst();
        carDirectionBox.getSelectionModel().selectFirst();
    }

    private ListCell<RequestStatus> requestStatusCell() {
        return new ListCell<>() {
            @Override protected void updateItem(RequestStatus status, boolean empty) {
                super.updateItem(status, empty);
                setText(empty ? null : status == null ? "Все" : status.getDisplayName());
            }
        };
    }

    @FXML private void onApplyRequests() {
        perform(() -> applyRequests(new TableQuery(requestSearchField.getText(), requestStatusBox.getValue(),
                minPriceField.getText(), maxPriceField.getText(), requestSortBox.getValue(),
                requestDirectionBox.getSelectionModel().getSelectedIndex() == 0)));
    }

    private void applyRequests(TableQuery query) {
        List<PurchaseRequest> result = requestService.findForTable(query);
        requestQuery = query;
        requestsTable.getSortOrder().clear();
        requests.setAll(result);
    }

    @FXML private void onResetRequests() {
        perform(() -> {
            applyRequests(TableQuery.empty());
            requestSearchField.clear();
            requestStatusBox.getSelectionModel().selectFirst();
            minPriceField.clear();
            maxPriceField.clear();
            requestSortBox.setValue(SortField.DEFAULT);
            requestDirectionBox.getSelectionModel().selectFirst();
        });
    }

    @FXML private void onApplyCars() {
        perform(() -> applyCars(new TableQuery(carSearchField.getText(), null, null, null,
                carSortBox.getValue(), carDirectionBox.getSelectionModel().getSelectedIndex() == 0)));
    }

    private void applyCars(TableQuery query) {
        List<Car> result = carService.findForTable(query);
        carQuery = query;
        carsTable.getSortOrder().clear();
        cars.setAll(result);
    }

    @FXML private void onResetCars() {
        perform(() -> {
            applyCars(TableQuery.empty());
            carSearchField.clear();
            carSortBox.setValue(SortField.DEFAULT);
            carDirectionBox.getSelectionModel().selectFirst();
        });
    }

    public void refreshAll() {
        // Ошибка БД не должна очищать уже показанные данные.
        List<PurchaseRequest> freshRequests = requestService.findForTable(requestQuery);
        List<Car> freshCars = carService.findForTable(carQuery);
        List<User> freshUsers = userService.findAll();
        requests.setAll(freshRequests);
        cars.setAll(freshCars);
        users.setAll(freshUsers);
        requestsTable.sort();
        carsTable.sort();
        usersTable.sort();
    }

    @FXML private void onRefresh() {
        perform(this::refreshAll);
    }

    @FXML private void onAddRequest() {
        perform(() -> openRequest(null));
    }

    @FXML private void onRequestDetails() {
        PurchaseRequest request = selected(requestsTable, "Выберите заявку для просмотра подробностей.");
        if (request != null) {
            perform(() -> {
                RequestDetails details = requestService.getDetails(request.getId());
                DialogHelper.show("/fxml/request-details.fxml", "Заявка №" + request.getId(), owner(),
                        (RequestDetailsController controller) -> controller.setup(details));
            });
        }
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
        requests.setAll(requestService.findForTable(requestQuery));
        requestsTable.sort();
        selectById(requestsTable, id, PurchaseRequest::getId);
    }

    @FXML private void onAddCar() {
        perform(() -> openCar(null));
    }

    @FXML private void onCarRequests() {
        Car selected = selected(carsTable, "Выберите автомобиль для просмотра заявок.");
        if (selected != null) {
            perform(() -> {
                Car car = carService.findById(selected.getId());
                List<PurchaseRequest> relatedRequests = requestService.findByCarId(car.getId());
                DialogHelper.show("/fxml/related-data.fxml", "Заявки на автомобиль", owner(),
                        (RelatedDataController controller) -> controller.setupForCar(car, relatedRequests));
            });
        }
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

    @FXML private void onAddUser() {
        perform(() -> openUser(null));
    }

    @FXML private void onUserRelations() {
        User selected = selected(usersTable, "Выберите пользователя для просмотра связанных данных.");
        if (selected != null) {
            perform(() -> {
                User user = userService.findById(selected.getId());
                List<Car> relatedCars = carService.findBySellerId(user.getId());
                List<PurchaseRequest> relatedRequests = requestService.findByUserId(user.getId());
                DialogHelper.show("/fxml/related-data.fxml", "Автомобили и заявки пользователя", owner(),
                        (RelatedDataController controller) -> controller.setupForUser(user, relatedCars, relatedRequests));
            });
        }
    }

    @FXML private void onEditUser() {
        User user = selected(usersTable, "Выберите пользователя для изменения.");
        if (user != null) {
            perform(() -> openUser(userService.findById(user.getId())));
        }
    }

    private void openUser(User user) {
        UserDialogController dialog = DialogHelper.show("/fxml/user-dialog.fxml",
                user == null ? "Добавление пользователя" : "Редактирование пользователя", owner(),
                (UserDialogController controller) -> controller.setup(user));
        if (dialog.getSavedId() != null) {
            refreshAll();
            selectById(usersTable, dialog.getSavedId(), User::getId);
        }
    }

    @FXML private void onDeleteUser() {
        User user = selected(usersTable, "Выберите пользователя для удаления.");
        if (user != null && AlertHelper.confirm(owner(), "Удалить пользователя «" + user.getFullName()
                + "» (" + user.getLogin() + ")?")) {
            perform(() -> {
                userService.delete(user.getId());
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
