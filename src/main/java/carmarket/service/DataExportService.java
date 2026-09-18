package carmarket.service;

import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DataExportService {

    public void exportToExcel(Path file,
                              List<User> users,
                              List<Car> cars,
                              List<PurchaseRequest> requests) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream output = Files.newOutputStream(file)) {
                writeUsers(workbook.createSheet("Users"), users);
                writeCars(workbook.createSheet("Cars"), cars);
                writeRequests(workbook.createSheet("PurchaseRequests"), requests);
                workbook.write(output);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось экспортировать данные: " + e.getMessage(), e);
        }
    }

    private void writeUsers(Sheet sheet, List<User> users) {
        addRow(sheet, 0, "ID", "ФИО", "Логин", "Роль", "Дата регистрации");
        int rowIndex = 1;
        for (User user : users) {
            addRow(sheet, rowIndex++, user.getId(), user.getFullName(), user.getLogin(),
                    user.getRole(), user.getCreatedAt());
        }
        autosize(sheet, 6);
    }

    private void writeCars(Sheet sheet, List<Car> cars) {
        addRow(sheet, 0, "ID", "ID продавца", "Марка", "Модель", "Год", "Пробег",
                "Цена", "VIN", "Тип кузова", "Коробка передач", "Тип топлива",
            "Объем двигателя", "Описание", "Статус", "Дата создания");
        int rowIndex = 1;
        for (Car car : cars) {
            addRow(sheet, rowIndex++, car.getId(), car.getSellerId(), car.getBrand(),
                    car.getModel(), car.getYear(), car.getMileage(), car.getPrice(),
                    car.getVin(), car.getBodyType(), car.getTransmission(), car.getFuelType(),
                    car.getEngineVolume(), car.getDescription(), car.getStatus(), car.getCreatedAt());
        }
        autosize(sheet, 15);
    }

    private void writeRequests(Sheet sheet, List<PurchaseRequest> requests) {
        addRow(sheet, 0, "ID", "ID пользователя", "ID автомобиля", "Сообщение",
                "Статус", "Дата создания", "Дата изменения");
        int rowIndex = 1;
        for (PurchaseRequest request : requests) {
            addRow(sheet, rowIndex++, request.getId(), request.getUserId(), request.getCarId(),
                    request.getMessage(), request.getStatus(), request.getCreatedAt(),
                    request.getUpdatedAt());
        }
        autosize(sheet, 8);
    }

    private void addRow(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            Object value = values[columnIndex];
            row.createCell(columnIndex).setCellValue(value == null ? "" : value.toString());
        }
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }
    }
}