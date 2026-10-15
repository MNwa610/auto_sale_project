package carmarket.fx;

import carmarket.exception.DatabaseException;
import carmarket.repository.CarRepository;
import carmarket.repository.PurchaseRequestRepository;
import carmarket.repository.UserRepository;
import carmarket.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CarMarketApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(CarMarketApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                CarMarketApplication.class.getResource("/fxml/main-view.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 640);
        scene.getStylesheets().add(
                CarMarketApplication.class.getResource("/css/style.css").toExternalForm()
        );

        stage.setTitle("Маркетплейс автомобилей с пробегом");
        stage.setScene(scene);
        stage.show();

        checkDatabase();
    }

    private void checkDatabase() {
        try {
            DatabaseManager.checkConnection();

            int userCount = new UserRepository().findAll().size();
            int carCount = new CarRepository().findAll().size();
            int requestCount = new PurchaseRequestRepository().findAll().size();

            log.info("Репозитории работают. Пользователей: {}, автомобилей: {}, заявок: {}",
                    userCount, carCount, requestCount);
        } catch (DatabaseException e) {
            log.error("Ошибка при подключении к базе данных", e);
            showDatabaseError();
        }
    }

    private void showDatabaseError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка подключения");
        alert.setHeaderText("Не удалось подключиться к базе данных");
        alert.setContentText(
                "Проверьте:\n"
                        + "1. Запущен ли PostgreSQL.\n"
                        + "2. Верны ли db.url, db.user и db.password в файле database.properties "
                        + "(скопируйте его из database.properties.example).\n"
                        + "Обычно база: localhost:5432/auto_sale."
        );
        alert.showAndWait();
    }
}
