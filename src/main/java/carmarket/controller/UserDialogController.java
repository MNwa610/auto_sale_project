package carmarket.controller;

import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.DatabaseException;
import carmarket.model.User;
import carmarket.service.UserService;
import carmarket.util.AlertHelper;
import carmarket.util.DialogHelper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class UserDialogController {
    private final UserService service = new UserService();
    private Long userId;
    private Long savedId;

    @FXML private TextField fullNameField;
    @FXML private TextField loginField;
    @FXML private ComboBox<UserRole> roleBox;
    @FXML private PasswordField passwordField;
    @FXML private Label passwordHint;

    public void setup(User user) {
        DialogHelper.display(roleBox, UserRole::getDisplayName);
        roleBox.getItems().setAll(UserRole.values());
        roleBox.setValue(UserRole.USER);
        passwordField.clear();
        passwordHint.setText("Не менее " + UserService.MIN_PASSWORD_LENGTH + " символов.");
        if (user != null) {
            userId = user.getId();
            fullNameField.setText(user.getFullName());
            loginField.setText(user.getLogin());
            roleBox.setValue(user.getRole());
            passwordHint.setText("Оставьте пустым, чтобы сохранить текущий пароль. Новый пароль — от "
                    + UserService.MIN_PASSWORD_LENGTH + " символов.");
        }
    }

    @FXML
    private void onSave() {
        try {
            savedId = service.saveForm(userId, fullNameField.getText(), loginField.getText(),
                    roleBox.getValue(), passwordField.getText()).getId();
            onCancel();
        } catch (BusinessException | DatabaseException e) {
            AlertHelper.error(fullNameField.getScene().getWindow(), e);
        }
    }

    @FXML
    private void onCancel() {
        passwordField.clear();
        ((Stage) fullNameField.getScene().getWindow()).close();
    }

    public Long getSavedId() {
        return savedId;
    }
}
