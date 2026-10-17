package carmarket.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.Parent;
import carmarket.enums.RequestStatus;
import carmarket.enums.SortField;
import carmarket.enums.UserRole;
import carmarket.model.User;
import carmarket.model.PurchaseRequest;
import carmarket.model.Car;
import carmarket.service.CarService;
import carmarket.service.PurchaseRequestService;
import carmarket.service.UserService;
import carmarket.service.InMemoryCarRepository;
import carmarket.service.InMemoryPurchaseRequestRepository;
import carmarket.service.InMemoryUserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FxmlViewTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        FutureTask<Void> started = new FutureTask<>(() -> null);
        Platform.startup(started);
        started.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void allViews_shouldLoadWithControllersAndEventHandlers() throws Exception {
        FutureTask<Void> load = new FutureTask<>(() -> {
            for (String resource : new String[]{"main-view.fxml", "request-dialog.fxml", "car-dialog.fxml", "user-dialog.fxml"}) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + resource));
                assertThat((Object) loader.load()).isNotNull();
                assertThat((Object) loader.getController()).isNotNull();
            }
            return null;
        });
        Platform.runLater(load);
        load.get(10, TimeUnit.SECONDS);
    }

    @Test
    void userDialog_shouldNeverFillPasswordWhenEditing() throws Exception {
        FutureTask<Void> check = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user-dialog.fxml"));
            loader.load();
            User user = new User("Иван", "ivan", "stored-password", UserRole.USER);
            user.setId(1L);
            UserDialogController controller = loader.getController();
            controller.setup(user);
            PasswordField password = (PasswordField) loader.getNamespace().get("passwordField");
            assertThat(password.getText()).isEmpty();
            return null;
        });
        Platform.runLater(check);
        check.get(10, TimeUnit.SECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryControls_shouldApplyResetAndKeepHeaderSortingAvailable() throws Exception {
        FutureTask<Void> check = new FutureTask<>(() -> {
            InMemoryPurchaseRequestRepository requests = new InMemoryPurchaseRequestRepository();
            InMemoryCarRepository cars = new InMemoryCarRepository();
            InMemoryUserRepository users = new InMemoryUserRepository();
            for (int i = 1; i <= 3; i++) {
                PurchaseRequest request = new PurchaseRequest(1L, 1L, "Осмотр", RequestStatus.APPROVED);
                request.setCarBrand(i == 3 ? "BMW" : "Toyota");
                request.setCarModel("Модель");
                request.setUserFullName("Иван");
                request.setCarPrice(BigDecimal.valueOf(i * 500000L));
                request.setCarMileage(i * 10000);
                requests.seed(request);
                Car car = new Car();
                car.setBrand(request.getCarBrand());
                car.setModel("Модель");
                car.setPrice(request.getCarPrice());
                car.setMileage(request.getCarMileage());
                cars.seed(car);
            }
            MainController controller = new MainController(new PurchaseRequestService(requests, users, cars),
                    new CarService(cars, users), new UserService(users));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            loader.setControllerFactory(type -> controller);
            Parent root = loader.load();
            new Scene(root);
            controller.refreshAll();

            var nodes = loader.getNamespace();
            TextField search = (TextField) nodes.get("requestSearchField");
            ComboBox<RequestStatus> status = (ComboBox<RequestStatus>) nodes.get("requestStatusBox");
            ComboBox<SortField> sort = (ComboBox<SortField>) nodes.get("requestSortBox");
            ComboBox<String> direction = (ComboBox<String>) nodes.get("requestDirectionBox");
            TableView<PurchaseRequest> table = (TableView<PurchaseRequest>) nodes.get("requestsTable");
            TableColumn<PurchaseRequest, BigDecimal> priceColumn =
                    (TableColumn<PurchaseRequest, BigDecimal>) nodes.get("requestPriceColumn");
            TextField min = (TextField) nodes.get("minPriceField");
            search.setText("Toyota");
            status.setValue(RequestStatus.APPROVED);
            min.setText("500000");
            sort.setValue(SortField.MILEAGE);
            direction.getSelectionModel().select(1);
            ((Button) nodes.get("applyRequestsButton")).fire();
            assertThat(table.getItems()).extracting(PurchaseRequest::getId).containsExactly(2L, 1L);

            priceColumn.setSortType(TableColumn.SortType.ASCENDING);
            table.getSortOrder().setAll(priceColumn);
            assertThat(table.getItems()).extracting(PurchaseRequest::getId).containsExactly(1L, 2L);
            ((Button) nodes.get("applyRequestsButton")).fire();
            assertThat(table.getSortOrder()).isEmpty();
            assertThat(table.getItems()).extracting(PurchaseRequest::getId).containsExactly(2L, 1L);
            search.setText("BMW");
            controller.refreshAll();
            assertThat(table.getItems()).extracting(PurchaseRequest::getId).containsExactly(2L, 1L);
            ((Button) nodes.get("resetRequestsButton")).fire();
            assertThat(table.getItems()).extracting(PurchaseRequest::getId).containsExactly(1L, 2L, 3L);
            assertThat(search.getText()).isEmpty();
            assertThat(min.getText()).isEmpty();
            assertThat(status.getValue()).isNull();
            assertThat(sort.getValue()).isEqualTo(SortField.DEFAULT);
            assertThat(direction.getSelectionModel().getSelectedIndex()).isZero();

            TextField carSearch = (TextField) nodes.get("carSearchField");
            TableView<Car> carTable = (TableView<Car>) nodes.get("carsTable");
            carSearch.setText("BMW");
            ((Button) nodes.get("applyCarsButton")).fire();
            assertThat(carTable.getItems()).extracting(Car::getId).containsExactly(3L);
            ((Button) nodes.get("resetCarsButton")).fire();
            assertThat(carTable.getItems()).extracting(Car::getId).containsExactly(1L, 2L, 3L);
            return null;
        });
        Platform.runLater(check);
        check.get(10, TimeUnit.SECONDS);
    }
}
