package carmarket.ui;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.UserRole;
import carmarket.model.Brand;
import carmarket.model.Car;
import carmarket.model.CarModel;
import carmarket.model.CarSpecification;
import carmarket.model.NamedCatalogItem;
import carmarket.model.PurchaseRequest;
import carmarket.model.Role;
import carmarket.model.User;
import carmarket.service.SystemStatistics;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ConsoleView {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DecimalFormat PRICE_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        PRICE_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private ConsoleView() {
    }

    public static void blankLine() {
        System.out.println();
    }

    public static void printFrameTitle(String title) {
        blankLine();
        System.out.println("===============================================");
        System.out.println("  " + title);
        System.out.println("===============================================");
    }

    public static void printSection(String title) {
        blankLine();
        System.out.println("--- " + title + " ---");
    }

    public static void printMenuOption(int number, String label) {
        System.out.println(number + ". " + label);
    }

    public static void printMessage(String message) {
        System.out.println(message);
    }

    public static void printSuccess(String message) {
        System.out.println(message);
    }

    public static void printError(String message) {
        System.out.println("Ошибка: " + message);
    }

    public static void printCars(List<Car> cars) {
        if (cars.isEmpty()) {
            printMessage("Автомобили не найдены.");
            return;
        }

        for (Car car : cars) {
            System.out.println(formatCarLine(car));
        }
        blankLine();
        printMessage("Найдено: " + cars.size());
    }

    public static void printCar(Car car) {
        printSection("Автомобиль #" + car.getId());
        printDetailLine("Марка / модель", car.getBrand() + " " + car.getModel());
        printDetailLine("Год выпуска", String.valueOf(car.getYear()));
        printDetailLine("Пробег", formatMileage(car.getMileage()) + " км");
        printDetailLine("Цена", formatPrice(car.getPrice()) + " руб.");
        printDetailLine("VIN", nullToDash(car.getVin()));
        printDetailLine("Кузов", nullToDash(car.getBodyType()));
        printDetailLine("КПП", nullToDash(car.getTransmission()));
        printDetailLine("Топливо", nullToDash(car.getFuelType()));
        printDetailLine("Объём двигателя", formatEngineVolume(car.getEngineVolume()));
        printDetailLine("Статус", formatCarStatus(car.getStatus()));
        printDetailLine("ID продавца", str(car.getSellerId()));
        if (car.getDescription() != null && !car.getDescription().isBlank()) {
            printDetailLine("Описание", car.getDescription());
        }
        if (car.getCreatedAt() != null) {
            printDetailLine("Добавлен", car.getCreatedAt().format(DATE_TIME));
        }
    }

    public static void printUsers(List<User> users) {
        if (users.isEmpty()) {
            printMessage("Пользователи не найдены.");
            return;
        }

        for (User user : users) {
            String registered = user.getCreatedAt() != null
                    ? user.getCreatedAt().format(DATE_TIME)
                    : "-";
            System.out.printf(
                    "ID %s | %s | логин: %s | %s | с %s%n",
                    str(user.getId()),
                    nullToDash(user.getFullName()),
                    nullToDash(user.getLogin()),
                    formatUserRole(user.getRole()),
                    registered
            );
        }
        blankLine();
        printMessage("Найдено: " + users.size());
    }

    public static void printUser(User user) {
        printSection("Пользователь #" + user.getId());
        printDetailLine("ФИО", user.getFullName());
        printDetailLine("Логин", user.getLogin());
        printDetailLine("Роль", formatUserRole(user.getRole()));
        if (user.getCreatedAt() != null) {
            printDetailLine("Регистрация", user.getCreatedAt().format(DATE_TIME));
        }
    }

    public static void printRequests(List<PurchaseRequest> requests) {
        if (requests.isEmpty()) {
            printMessage("Заявки не найдены.");
            return;
        }

        for (PurchaseRequest request : requests) {
            String created = request.getCreatedAt() != null
                    ? request.getCreatedAt().format(DATE_TIME)
                    : "-";
            System.out.printf(
                    "ID %s | пользователь %s | авто %s | %s | %s%n",
                    str(request.getId()),
                    str(request.getUserId()),
                    str(request.getCarId()),
                    formatRequestStatus(request.getStatus()),
                    created
            );
        }
        blankLine();
        printMessage("Найдено: " + requests.size());
    }

    public static void printRequest(PurchaseRequest request) {
        printSection("Заявка #" + request.getId());
        printDetailLine("ID пользователя", str(request.getUserId()));
        printDetailLine("ID автомобиля", str(request.getCarId()));
        printDetailLine("Статус", formatRequestStatus(request.getStatus()));
        printDetailLine("Сообщение", messageOrEmpty(request.getMessage()));
        if (request.getCreatedAt() != null) {
            printDetailLine("Создана", request.getCreatedAt().format(DATE_TIME));
        }
        if (request.getUpdatedAt() != null) {
            printDetailLine("Обновлена", request.getUpdatedAt().format(DATE_TIME));
        }
    }

    public static void printRoles(List<Role> roles) {
        if (roles.isEmpty()) {
            printMessage("Роли не найдены.");
            return;
        }
        for (Role role : roles) {
            System.out.printf("ID %s | %s | %s%n", str(role.getId()), role.getCode(), role.getName());
        }
        blankLine();
        printMessage("Найдено: " + roles.size());
    }

    public static void printBrands(List<Brand> brands) {
        if (brands.isEmpty()) {
            printMessage("Марки не найдены.");
            return;
        }
        for (Brand brand : brands) {
            System.out.printf("ID %s | %s%n", str(brand.getId()), nullToDash(brand.getName()));
        }
        blankLine();
        printMessage("Найдено: " + brands.size());
    }

    public static void printCarModels(List<CarModel> models) {
        if (models.isEmpty()) {
            printMessage("Модели не найдены.");
            return;
        }
        for (CarModel model : models) {
            System.out.printf("ID %s | %s | %s%n",
                    str(model.getId()),
                    nullToDash(model.getBrandName()),
                    nullToDash(model.getName()));
        }
        blankLine();
        printMessage("Найдено: " + models.size());
    }

    public static void printCatalogItems(List<NamedCatalogItem> items, String emptyMessage) {
        if (items.isEmpty()) {
            printMessage(emptyMessage);
            return;
        }
        for (NamedCatalogItem item : items) {
            System.out.printf("ID %s | %s%n", str(item.getId()), nullToDash(item.getName()));
        }
        blankLine();
        printMessage("Найдено: " + items.size());
    }

    public static void printSpecifications(List<CarSpecification> specifications) {
        if (specifications.isEmpty()) {
            printMessage("Характеристики не найдены.");
            return;
        }
        for (CarSpecification specification : specifications) {
            System.out.printf(
                    "Авто %s | VIN %s | %s | %s | %s | %s л%n",
                    str(specification.getCarId()),
                    nullToDash(specification.getVin()),
                    nullToDash(specification.getBodyType()),
                    nullToDash(specification.getTransmission()),
                    nullToDash(specification.getFuelType()),
                    specification.getEngineVolume() == null
                            ? "-"
                            : specification.getEngineVolume().stripTrailingZeros().toPlainString()
            );
        }
        blankLine();
        printMessage("Найдено: " + specifications.size());
    }

    public static void printStatistics(SystemStatistics statistics) {
        printSection("Статистика системы");
        printDetailLine("Всего пользователей", String.valueOf(statistics.totalUsers()));
        printDetailLine("Всего автомобилей", String.valueOf(statistics.totalCars()));
        printDetailLine("Доступных автомобилей", String.valueOf(statistics.availableCars()));
        printDetailLine("Проданных автомобилей", String.valueOf(statistics.soldCars()));
        printDetailLine("Всего заявок", String.valueOf(statistics.totalRequests()));
        printDetailLine("Активных заявок", String.valueOf(statistics.activeRequests()));
        printDetailLine("Завершенных заявок", String.valueOf(statistics.completedRequests()));
        printDetailLine("Отмененных заявок", String.valueOf(statistics.cancelledRequests()));
    }

    public static void printRequestStatusMenu() {
        blankLine();
        System.out.println("Статусы заявок:");
        for (RequestStatus status : RequestStatus.values()) {
            System.out.printf("  %s - %s%n", status.name(), formatRequestStatus(status));
        }
    }

    private static String formatCarLine(Car car) {
        return String.format(
                "ID %s | %s %s | %d г. | %s км | %s руб. | %s",
                str(car.getId()),
                nullToDash(car.getBrand()),
                nullToDash(car.getModel()),
                car.getYear(),
                formatMileage(car.getMileage()),
                formatPrice(car.getPrice()),
                formatCarStatus(car.getStatus())
        );
    }

    private static void printDetailLine(String label, String value) {
        System.out.println("  " + label + ": " + value);
    }

    private static String str(Long value) {
        return value != null ? String.valueOf(value) : "-";
    }

    private static String nullToDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private static String messageOrEmpty(String message) {
        return message != null && !message.isBlank() ? message : "-";
    }

    private static String formatMileage(int mileage) {
        return String.format("%,d", mileage).replace(',', ' ');
    }

    private static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "-";
        }
        return PRICE_FORMAT.format(price);
    }

    private static String formatEngineVolume(BigDecimal volume) {
        if (volume == null) {
            return "-";
        }
        return volume.stripTrailingZeros().toPlainString() + " л";
    }

    static String formatCarStatus(CarStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case AVAILABLE -> "В продаже";
            case RESERVED -> "Забронирован";
            case SOLD -> "Продан";
            case ARCHIVED -> "В архиве";
        };
    }

    static String formatRequestStatus(RequestStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case NEW -> "Новая";
            case IN_PROGRESS -> "В работе";
            case APPROVED -> "Одобрена";
            case REJECTED -> "Отклонена";
            case CANCELLED -> "Отменена";
        };
    }

    private static String formatUserRole(UserRole role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case USER -> "Пользователь";
            case ADMIN -> "Администратор";
        };
    }
}
