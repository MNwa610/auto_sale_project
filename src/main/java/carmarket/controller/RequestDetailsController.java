package carmarket.controller;

import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.RequestDetails;
import carmarket.model.User;
import carmarket.util.TableHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class RequestDetailsController {
    @FXML private Label titleLabel;
    @FXML private TextArea detailsArea;

    public void setup(RequestDetails details) {
        PurchaseRequest request = details.request();
        User user = details.user();
        Car car = details.car();
        titleLabel.setText("Заявка №" + request.getId());
        detailsArea.setText("""
                Пользователь
                ФИО: %s
                Логин: %s
                Роль: %s

                Автомобиль
                Марка и модель: %s %s
                Год: %s
                Пробег: %s км
                Цена: %s ₽
                Статус: %s
                VIN: %s
                Продавец: %s
                Кузов: %s
                Коробка передач: %s
                Топливо: %s
                Объём двигателя: %s л
                Описание: %s

                Заявка
                Статус: %s
                Создана: %s
                Обновлена: %s
                Сообщение:
                %s
                """.formatted(user.getFullName(), user.getLogin(), user.getRole().getDisplayName(),
                car.getBrand(), car.getModel(), car.getYear(), car.getMileage(), car.getPrice().toPlainString(),
                car.getStatus().getDisplayName(), text(car.getVin()), text(car.getSellerFullName()),
                text(car.getBodyType()), text(car.getTransmission()), text(car.getFuelType()),
                text(car.getEngineVolume()), text(car.getDescription()), request.getStatus().getDisplayName(),
                TableHelper.formatDate(request.getCreatedAt()), TableHelper.formatDate(request.getUpdatedAt()),
                text(request.getMessage())));
        detailsArea.positionCaret(0);
    }

    private String text(Object value) {
        return value == null || value.toString().isBlank() ? "—" : value.toString();
    }

    @FXML
    private void onClose() {
        ((Stage) detailsArea.getScene().getWindow()).close();
    }
}
