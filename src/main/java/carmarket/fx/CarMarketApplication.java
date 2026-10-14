package carmarket.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CarMarketApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                CarMarketApplication.class.getResource("/fxml/main-view.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
                CarMarketApplication.class.getResource("/css/style.css").toExternalForm()
        );

        stage.setTitle("Маркетплейс автомобилей с пробегом");
        stage.setScene(scene);
        stage.show();
    }
}
