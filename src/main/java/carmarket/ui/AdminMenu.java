package carmarket.ui;

import carmarket.enums.RequestStatus;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.service.CarService;
import carmarket.service.DataExportService;
import carmarket.service.PurchaseRequestService;
import carmarket.service.StatisticsService;
import carmarket.service.SystemStatistics;
import carmarket.service.UserService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

public class AdminMenu {

    private final UserService userService;
    private final CarService carService;
    private final PurchaseRequestService requestService;
    private final InputHelper inputHelper;
    private final StatisticsService statisticsService;
    private final DataExportService dataExportService;

    public AdminMenu(UserService userService,
                     CarService carService,
                     PurchaseRequestService requestService,
                     InputHelper inputHelper) {
        this.userService = userService;
        this.carService = carService;
        this.requestService = requestService;
        this.inputHelper = inputHelper;
        this.statisticsService = new StatisticsService();
        this.dataExportService = new DataExportService();
    }

    public void show() {
        boolean running = true;

        while (running) {
            printMenu();
            int choice = inputHelper.readInt("Выберите пункт: ");

            try {
                switch (choice) {
                    case 1 -> usersMenu();
                    case 2 -> carsMenu();
                    case 3 -> requestsMenu();
                    case 4 -> searchMenu();
                    case 5 -> filterMenu();
                    case 6 -> sortMenu();
                        case 7 -> statisticsMenu();
                        case 8 -> exportMenu();
                    case 9 -> databaseTablesMenu();

                    case 10 -> running = false;
                    default -> System.out.println("Ошибка: такого пункта меню нет.");
                }
            } catch (RuntimeException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("================================================");
        System.out.println("       МАРКЕТПЛЕЙС АВТОМОБИЛЕЙ С ПРОБЕГОМ");
        System.out.println("================================================");
        System.out.println("1. Пользователи");
        System.out.println("2. Автомобили");
        System.out.println("3. Заявки");
        System.out.println("4. Поиск");
        System.out.println("5. Фильтрация");
        System.out.println("6. Сортировка");
        System.out.println("7. Статистика");
        System.out.println("8. Экспорт");
        System.out.println("9. Таблицы БД");
        System.out.println("10. Выйти");
    }

    private void usersMenu() {
        System.out.println();
        System.out.println("--- ПОЛЬЗОВАТЕЛИ ---");
        System.out.println("1. Показать всех");
        System.out.println("2. Найти по ID");
        System.out.println("3. Назад");

        int choice = inputHelper.readInt("Выберите пункт: ");

        switch (choice) {
            case 1 -> printUsers(userService.findAll());
            case 2 -> {
                long id = inputHelper.readPositiveLong("Введите ID пользователя: ");
                System.out.println(userService.findById(id));
            }
            case 3 -> {
            }
            default -> System.out.println("Ошибка: такого пункта нет.");
        }
    }

    private void carsMenu() {
        System.out.println();
        System.out.println("--- АВТОМОБИЛИ ---");
        System.out.println("1. Показать все");
        System.out.println("2. Найти по ID");
        System.out.println("3. Назад");

        int choice = inputHelper.readInt("Выберите пункт: ");

        switch (choice) {
            case 1 -> printCars(carService.findAll());
            case 2 -> {
                long id = inputHelper.readPositiveLong("Введите ID автомобиля: ");
                System.out.println(carService.findById(id));
            }
            case 3 -> {
            }
            default -> System.out.println("Ошибка: такого пункта нет.");
        }
    }

    private void requestsMenu() {
        System.out.println();
        System.out.println("--- ЗАЯВКИ ---");
        System.out.println("1. Показать все");
        System.out.println("2. Найти по ID");
        System.out.println("3. Изменить статус");
        System.out.println("4. Назад");

        int choice = inputHelper.readInt("Выберите пункт: ");

        switch (choice) {
            case 1 -> printRequests(requestService.findAll());
            case 2 -> {
                long id = inputHelper.readPositiveLong("Введите ID заявки: ");
                System.out.println(requestService.findById(id));
            }
            case 3 -> changeRequestStatus();
            case 4 -> {
            }
            default -> System.out.println("Ошибка: такого пункта нет.");
        }
    }

    private void changeRequestStatus() {
        long id = inputHelper.readPositiveLong("Введите ID заявки: ");
        RequestStatus status = readRequestStatus();

        PurchaseRequest request = requestService.findById(id);
        request.setStatus(status);
        requestService.update(request);

        System.out.println("Статус заявки изменён.");
    }

    private void searchMenu() {
        System.out.println();
        System.out.println("1. Поиск автомобиля по марке/модели");
        System.out.println("2. Поиск заявок по имени пользователя");

        int choice = inputHelper.readInt("Выберите пункт: ");

        if (choice == 1) {
            String query = inputHelper.readString("Введите марку или модель: ");
            printCars(carService.search(query));
        } else if (choice == 2) {
            String query = inputHelper.readString("Введите имя пользователя: ");
            printRequests(requestService.searchByUserName(query));
        } else {
            System.out.println("Ошибка: такого пункта нет.");
        }
    }

    private void filterMenu() {
        System.out.println();
        System.out.println("1. Заявки по статусу");
        System.out.println("2. Автомобили по диапазону цены");

        int choice = inputHelper.readInt("Выберите пункт: ");

        if (choice == 1) {
            RequestStatus status = readRequestStatus();
            printRequests(requestService.filterByStatus(status));
        } else if (choice == 2) {
            BigDecimal min =
                    inputHelper.readNonNegativePrice("Минимальная цена: ");
            BigDecimal max =
                    inputHelper.readNonNegativePrice("Максимальная цена: ");

            printCars(carService.filterByPrice(min, max));
        } else {
            System.out.println("Ошибка: такого пункта нет.");
        }
    }

    private void sortMenu() {
        System.out.println();
        System.out.println("1. По цене");
        System.out.println("2. По пробегу");

        int choice = inputHelper.readInt("Выберите пункт: ");

        System.out.println("1. По возрастанию");
        System.out.println("2. По убыванию");

        int order = inputHelper.readInt("Выберите порядок: ");

        if (order != 1 && order != 2) {
            System.out.println("Ошибка: такого порядка сортировки нет.");
            return;
        }

        boolean ascending = order == 1;

        if (choice == 1) {
            printCars(carService.sortByPrice(ascending));
        } else if (choice == 2) {
            printCars(carService.sortByMileage(ascending));
        } else {
            System.out.println("Ошибка: такого пункта нет.");
        }
    }

    private void databaseTablesMenu() {
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("--- ТАБЛИЦЫ БД ---");
            System.out.println("1. Users");
            System.out.println("2. Cars");
            System.out.println("3. PurchaseRequests");
            System.out.println("4. Назад");

            int choice = inputHelper.readInt("Выберите пункт: ");

            switch (choice) {
                case 1 -> printUsers(userService.findAll());
                case 2 -> printCars(carService.findAll());
                case 3 -> printRequests(requestService.findAll());
                case 4 -> running = false;
                default -> System.out.println("Ошибка: такого пункта нет.");
            }
        }
    }

    private void statisticsMenu() {
        SystemStatistics statistics = statisticsService.calculate(
                userService.findAll(),
                carService.findAll(),
                requestService.findAll()
        );

        System.out.println();
        System.out.println("--- СТАТИСТИКА СИСТЕМЫ ---");
        System.out.println("Всего пользователей: " + statistics.totalUsers());
        System.out.println("Всего автомобилей: " + statistics.totalCars());
        System.out.println("Доступных автомобилей: " + statistics.availableCars());
        System.out.println("Проданных автомобилей: " + statistics.soldCars());
        System.out.println("Всего заявок: " + statistics.totalRequests());
        System.out.println("Активных заявок: " + statistics.activeRequests());
        System.out.println("Завершенных заявок: " + statistics.completedRequests());
        System.out.println("Отмененных заявок: " + statistics.cancelledRequests());
    }

    private void exportMenu() {
        String fileName = inputHelper.readString(
                "Путь к Excel-файлу (по умолчанию export.xlsx): "
        );
        if (fileName.isBlank()) {
            fileName = "export.xlsx";
        }
        if (!fileName.toLowerCase().endsWith(".xlsx")) {
            fileName += ".xlsx";
        }

        dataExportService.exportToExcel(
                Path.of(fileName),
                userService.findAll(),
                carService.findAll(),
                requestService.findAll()
        );
        System.out.println("Данные экспортированы в файл: " + Path.of(fileName).toAbsolutePath());
    }

    private RequestStatus readRequestStatus() {
        while (true) {
            String value = inputHelper.readString(
                    "Статус (NEW, IN_PROGRESS, APPROVED, REJECTED, CANCELLED): "
            );

            try {
                return RequestStatus.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: неизвестный статус.");
            }
        }
    }

    private void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("Пользователи не найдены.");
            return;
        }

        for (User user : users) {
            System.out.println(user);
        }
    }

    private void printCars(List<Car> cars) {
        if (cars.isEmpty()) {
            System.out.println("Автомобили не найдены.");
            return;
        }

        for (Car car : cars) {
            System.out.println(car);
        }
    }

    private void printRequests(List<PurchaseRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("Заявки не найдены.");
            return;
        }

        for (PurchaseRequest request : requests) {
            System.out.println(request);
        }
    }
}
