package carmarket.controller;

import carmarket.enums.RequestStatus;
import carmarket.exception.BusinessException;
import carmarket.exception.DatabaseException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.service.CarService;
import carmarket.service.PurchaseRequestService;
import carmarket.service.UserService;
import carmarket.util.AlertHelper;
import carmarket.util.DialogHelper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class RequestDialogController {
    private final PurchaseRequestService service = new PurchaseRequestService();
    private Long requestId;
    private Long savedId;
    private RequestStatus status = RequestStatus.NEW;

    @FXML private ComboBox<User> userBox;
    @FXML private ComboBox<Car> carBox;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;

    public void setup(PurchaseRequest request) {
        DialogHelper.display(userBox, user -> user.getFullName() + " (" + user.getLogin() + ")");
        DialogHelper.display(carBox, car -> car.getBrand() + " " + car.getModel() + ", "
                + car.getYear() + ", " + car.getPrice() + " ₽ (№" + car.getId() + ")");
        userBox.getItems().setAll(new UserService().findAll());
        carBox.getItems().setAll(new CarService().findAll());
        if (request != null) {
            requestId = request.getId();
            status = request.getStatus();
            userBox.getItems().stream().filter(u -> u.getId().equals(request.getUserId()))
                    .findFirst().ifPresent(userBox::setValue);
            carBox.getItems().stream().filter(c -> c.getId().equals(request.getCarId()))
                    .findFirst().ifPresent(carBox::setValue);
            messageArea.setText(request.getMessage());
        }
        statusLabel.setText(status.getDisplayName());
    }

    @FXML
    private void onSave() {
        PurchaseRequest request = new PurchaseRequest(
                userBox.getValue() == null ? null : userBox.getValue().getId(),
                carBox.getValue() == null ? null : carBox.getValue().getId(),
                messageArea.getText(), status);
        request.setId(requestId);
        try {
            PurchaseRequest saved = requestId == null ? service.create(request) : service.update(request);
            savedId = saved.getId();
            onCancel();
        } catch (BusinessException | DatabaseException e) {
            AlertHelper.error(messageArea.getScene().getWindow(), e);
        }
    }

    @FXML
    private void onCancel() {
        ((Stage) messageArea.getScene().getWindow()).close();
    }

    public Long getSavedId() {
        return savedId;
    }
}
