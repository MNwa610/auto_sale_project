package carmarket.controller;

import carmarket.enums.CarStatus;
import carmarket.exception.BusinessException;
import carmarket.exception.DatabaseException;
import carmarket.model.Brand;
import carmarket.model.Car;
import carmarket.model.CarModel;
import carmarket.model.NamedCatalogItem;
import carmarket.model.User;
import carmarket.service.CarService;
import carmarket.service.CatalogService;
import carmarket.service.UserService;
import carmarket.util.AlertHelper;
import carmarket.util.DialogHelper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Function;

public class CarDialogController {
    private final CarService service = new CarService();
    private final CatalogService catalogs = new CatalogService();
    private Long carId;
    private Long savedId;

    @FXML private ComboBox<User> sellerBox;
    @FXML private ComboBox<Brand> brandBox;
    @FXML private ComboBox<CarModel> modelBox;
    @FXML private ComboBox<CarStatus> statusBox;
    @FXML private ComboBox<NamedCatalogItem> bodyBox;
    @FXML private ComboBox<NamedCatalogItem> transmissionBox;
    @FXML private ComboBox<NamedCatalogItem> fuelBox;
    @FXML private TextField yearField;
    @FXML private TextField mileageField;
    @FXML private TextField priceField;
    @FXML private TextField vinField;
    @FXML private TextField engineField;
    @FXML private TextArea descriptionArea;

    public void setup(Car car) {
        DialogHelper.display(sellerBox, user -> user.getFullName() + " (" + user.getLogin() + ")");
        DialogHelper.display(brandBox, Brand::getName);
        DialogHelper.display(modelBox, CarModel::getName);
        DialogHelper.display(statusBox, CarStatus::getDisplayName);
        DialogHelper.display(bodyBox, NamedCatalogItem::getName);
        DialogHelper.display(transmissionBox, NamedCatalogItem::getName);
        DialogHelper.display(fuelBox, NamedCatalogItem::getName);
        sellerBox.getItems().setAll(new UserService().findAll());
        brandBox.getItems().setAll(catalogs.findBrands());
        bodyBox.getItems().setAll(catalogs.findBodyTypes());
        transmissionBox.getItems().setAll(catalogs.findTransmissions());
        fuelBox.getItems().setAll(catalogs.findFuelTypes());
        statusBox.getItems().setAll(CarStatus.values());
        statusBox.setValue(CarStatus.AVAILABLE);
        modelBox.setDisable(true);
        brandBox.valueProperty().addListener((observable, previous, selected) -> loadModels(selected));
        if (car != null) {
            carId = car.getId();
            sellerBox.getItems().stream().filter(user -> user.getId().equals(car.getSellerId()))
                    .findFirst().ifPresent(sellerBox::setValue);
            select(brandBox, car.getBrand(), Brand::getName);
            select(modelBox, car.getModel(), CarModel::getName);
            select(bodyBox, car.getBodyType(), NamedCatalogItem::getName);
            select(transmissionBox, car.getTransmission(), NamedCatalogItem::getName);
            select(fuelBox, car.getFuelType(), NamedCatalogItem::getName);
            statusBox.setValue(car.getStatus());
            yearField.setText(Integer.toString(car.getYear()));
            mileageField.setText(Integer.toString(car.getMileage()));
            priceField.setText(car.getPrice().toPlainString());
            vinField.setText(car.getVin());
            engineField.setText(car.getEngineVolume() == null ? "" : car.getEngineVolume().toPlainString());
            descriptionArea.setText(car.getDescription());
        }
    }

    private void loadModels(Brand brand) {
        modelBox.setValue(null);
        modelBox.getItems().clear();
        modelBox.setDisable(brand == null);
        if (brand != null) {
            try {
                modelBox.getItems().setAll(catalogs.findModelsByBrand(brand.getId()));
            } catch (DatabaseException e) {
                AlertHelper.error(brandBox.getScene().getWindow(), e);
            }
        }
    }

    private <T> void select(ComboBox<T> box, String name, Function<T, String> display) {
        box.getItems().stream().filter(item -> display.apply(item).equals(name))
                .findFirst().ifPresent(box::setValue);
    }

    @FXML
    private void onSave() {
        Car car = new Car();
        car.setId(carId);
        car.setSellerId(sellerBox.getValue() == null ? null : sellerBox.getValue().getId());
        car.setBrand(brandBox.getValue() == null ? null : brandBox.getValue().getName());
        car.setModel(modelBox.getValue() == null ? null : modelBox.getValue().getName());
        car.setStatus(statusBox.getValue());
        car.setBodyType(bodyBox.getValue() == null ? null : bodyBox.getValue().getName());
        car.setTransmission(transmissionBox.getValue() == null ? null : transmissionBox.getValue().getName());
        car.setFuelType(fuelBox.getValue() == null ? null : fuelBox.getValue().getName());
        car.setVin(vinField.getText());
        car.setDescription(descriptionArea.getText());
        try {
            savedId = service.saveForm(car, yearField.getText(), mileageField.getText(),
                    priceField.getText(), engineField.getText()).getId();
            onCancel();
        } catch (BusinessException | DatabaseException e) {
            AlertHelper.error(priceField.getScene().getWindow(), e);
        }
    }

    @FXML
    private void onCancel() {
        ((Stage) priceField.getScene().getWindow()).close();
    }

    public Long getSavedId() {
        return savedId;
    }
}
