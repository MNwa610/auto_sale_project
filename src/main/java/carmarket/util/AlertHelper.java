package carmarket.util;

import carmarket.exception.BusinessException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class AlertHelper {
    private static final Logger log = LoggerFactory.getLogger(AlertHelper.class);

    private AlertHelper() {
    }

    public static void information(Window owner, String message) {
        show(owner, Alert.AlertType.INFORMATION, "Информация", message);
    }

    public static void warning(Window owner, String message) {
        show(owner, Alert.AlertType.WARNING, "Внимание", message);
    }

    public static void error(Window owner, Exception exception) {
        String message;
        if (exception instanceof BusinessException) {
            message = exception.getMessage();
        } else {
            log.error("Не удалось выполнить действие", exception);
            message = exception instanceof carmarket.exception.DatabaseException
                    ? "Не удалось выполнить операцию с базой данных. Попробуйте ещё раз."
                    : "Не удалось открыть окно. Попробуйте ещё раз.";
        }
        show(owner, Alert.AlertType.ERROR, "Ошибка", message);
    }

    public static boolean confirm(Window owner, String message) {
        return confirm(owner, "Подтверждение удаления", message);
    }

    public static boolean confirm(Window owner, String title, String message) {
        ButtonType yes = new ButtonType("Да");
        ButtonType no = new ButtonType("Нет", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        configure(alert, owner, title);
        return alert.showAndWait().orElse(no) == yes;
    }

    public static <T> Optional<T> choose(Window owner, String title, List<T> options, Function<T, String> text) {
        List<String> labels = options.stream().map(text).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText("Новый статус:");
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        dialog.getDialogPane().getStylesheets().add(
                AlertHelper.class.getResource("/css/style.css").toExternalForm());
        return dialog.showAndWait().map(label -> options.get(labels.indexOf(label)));
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        configure(alert, owner, title);
        alert.showAndWait();
    }

    private static void configure(Alert alert, Window owner, String title) {
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStyleClass().add("app-dialog");
        alert.getDialogPane().getStylesheets().add(
                AlertHelper.class.getResource("/css/style.css").toExternalForm());
    }
}
