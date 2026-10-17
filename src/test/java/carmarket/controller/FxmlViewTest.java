package carmarket.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
            for (String resource : new String[]{"main-view.fxml", "request-dialog.fxml", "car-dialog.fxml"}) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + resource));
                assertThat((Object) loader.load()).isNotNull();
                assertThat((Object) loader.getController()).isNotNull();
            }
            return null;
        });
        Platform.runLater(load);
        load.get(10, TimeUnit.SECONDS);
    }
}
