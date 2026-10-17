package carmarket.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DialogHelper {
    private DialogHelper() {
    }

    public static <T> T show(String resource, String title, Window owner, Consumer<T> setup) {
        FXMLLoader loader = new FXMLLoader(DialogHelper.class.getResource(resource));
        try {
            Parent root = loader.load();
            T controller = loader.getController();
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(title);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(DialogHelper.class.getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setMinWidth(Math.max(520, root.minWidth(-1)));
            stage.setMinHeight(Math.max(280, root.minHeight(-1)));
            setup.accept(controller);
            stage.showAndWait();
            return controller;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> void display(ComboBox<T> combo, Function<T, String> text) {
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : text.apply(value);
            }

            @Override
            public T fromString(String value) {
                throw new UnsupportedOperationException("Выберите значение из списка");
            }
        });
    }
}
