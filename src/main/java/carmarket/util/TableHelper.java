package carmarket.util;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public final class TableHelper {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private TableHelper() {
    }

    public static <T, V> void column(TableColumn<T, V> column, Function<T, V> value) {
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(value.apply(cell.getValue())));
    }

    public static <T> void dateColumn(TableColumn<T, LocalDateTime> column, Function<T, LocalDateTime> value) {
        column(column, value);
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : formatDate(date));
            }
        });
    }

    public static String formatDate(LocalDateTime date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }
}
