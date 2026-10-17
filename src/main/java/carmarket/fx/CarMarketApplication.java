package carmarket.fx;

import carmarket.exception.DatabaseException;
import carmarket.controller.MainController;
import carmarket.util.AlertHelper;
import carmarket.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

        Scene scene = new Scene(root, 1100, 680);
        scene.getStylesheets().add(
                CarMarketApplication.class.getResource("/css/style.css").toExternalForm()
        );

        stage.setTitle("Маркетплейс автомобилей с пробегом");
        stage.setMinWidth(920);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();

        checkDatabase(stage, loader.getController());
    }

    private void checkDatabase(Stage stage, MainController controller) {
        try {
            DatabaseManager.checkConnection();

            controller.refreshAll();
        } catch (DatabaseException e) {
            log.error("Ошибка при подключении к базе данных", e);
            AlertHelper.error(stage, e);
        }
    }

}
