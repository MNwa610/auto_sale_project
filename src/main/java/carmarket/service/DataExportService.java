package carmarket.service;

import carmarket.exception.BusinessException;
import carmarket.model.Car;
import carmarket.model.ExportTable;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.util.CsvExporter;
import carmarket.util.ExcelExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DataExportService {
    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void exportRequests(Path file, List<PurchaseRequest> requests) {
        ExportTable table = new ExportTable("Заявки", List.of("№", "Пользователь", "Автомобиль",
                "Цена автомобиля, ₽", "Пробег, км", "Статус", "Сообщение", "Дата создания"),
                requests.stream().map(request -> row(text(request.getId()), request.getUserFullName(),
                        (text(request.getCarBrand()) + " " + text(request.getCarModel())).trim(),
                        request.getCarPrice(), request.getCarMileage(),
                        request.getStatus() == null ? "" : request.getStatus().getDisplayName(),
                        request.getMessage(), date(request.getCreatedAt()))).toList());
        export(file, table);
    }

    public void exportCars(Path file, List<Car> cars) {
        ExportTable table = new ExportTable("Автомобили", List.of("Марка", "Модель", "Год", "Пробег, км",
                "Цена, ₽", "Статус", "Продавец", "VIN"),
                cars.stream().map(car -> row(car.getBrand(), car.getModel(), car.getYear(), car.getMileage(),
                        car.getPrice(), car.getStatus() == null ? "" : car.getStatus().getDisplayName(),
                        car.getSellerFullName(), car.getVin())).toList());
        export(file, table);
    }

    public void exportUsers(Path file, List<User> users) {
        ExportTable table = new ExportTable("Пользователи", List.of("ФИО", "Логин", "Роль", "Дата регистрации"),
                users.stream().map(user -> row(user.getFullName(), user.getLogin(),
                        user.getRole() == null ? "" : user.getRole().getDisplayName(), date(user.getCreatedAt()))).toList());
        export(file, table);
    }

    public Path completeFileName(Path selected, String defaultExtension) {
        if (!List.of(".csv", ".xlsx").contains(defaultExtension)) {
            throw new BusinessException("Выберите формат CSV или XLSX.");
        }
        String name = selected.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") || name.endsWith(".xlsx")
                ? selected : selected.resolveSibling(selected.getFileName() + defaultExtension);
    }

    public boolean needsOverwriteConfirmation(Path selected, Path completed) {
        return !selected.toAbsolutePath().normalize().equals(completed.toAbsolutePath().normalize())
                && Files.exists(completed);
    }

    private void export(Path file, ExportTable table) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) {
            throw new BusinessException("Укажите расширение .csv или .xlsx в имени файла.");
        }
        write(file, List.of(table), name.endsWith(".csv"));
    }

    private void write(Path file, List<ExportTable> tables, boolean csv) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (csv) {
                new CsvExporter().export(file, tables.getFirst());
            } else {
                new ExcelExporter().export(file, tables);
            }
        } catch (IOException | SecurityException e) {
            log.error("Не удалось сохранить экспорт в {}", file, e);
            throw new BusinessException("Не удалось сохранить файл. Проверьте доступ к папке и убедитесь, "
                    + "что файл закрыт в Excel, затем попробуйте ещё раз.");
        }
    }

    // Сохраняем прежний экспорт трёх листов для совместимости с КР №1.
    public void exportToExcel(Path file, List<User> users, List<Car> cars, List<PurchaseRequest> requests) {
        ExportTable userTable = new ExportTable("Users", List.of("ID", "ФИО", "Логин", "Роль", "Дата регистрации"),
                users.stream().map(user -> textRow(user.getId(), user.getFullName(), user.getLogin(),
                        user.getRole(), user.getCreatedAt())).toList());
        ExportTable carTable = new ExportTable("Cars", List.of("ID", "ID продавца", "Марка", "Модель", "Год", "Пробег",
                "Цена", "VIN", "Тип кузова", "Коробка передач", "Тип топлива", "Объем двигателя", "Описание", "Статус", "Дата создания"),
                cars.stream().map(car -> textRow(car.getId(), car.getSellerId(), car.getBrand(), car.getModel(),
                        car.getYear(), car.getMileage(), car.getPrice(), car.getVin(), car.getBodyType(),
                        car.getTransmission(), car.getFuelType(), car.getEngineVolume(), car.getDescription(),
                        car.getStatus(), car.getCreatedAt())).toList());
        ExportTable requestTable = new ExportTable("PurchaseRequests", List.of("ID", "ID пользователя", "ID автомобиля",
                "Сообщение", "Статус", "Дата создания", "Дата изменения"),
                requests.stream().map(request -> textRow(request.getId(), request.getUserId(), request.getCarId(),
                        request.getMessage(), request.getStatus(), request.getCreatedAt(), request.getUpdatedAt())).toList());
        write(file, List.of(userTable, carTable, requestTable), false);
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private List<Object> textRow(Object... values) {
        return Arrays.stream(values).map(value -> (Object) text(value)).toList();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String date(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_FORMAT);
    }
}
