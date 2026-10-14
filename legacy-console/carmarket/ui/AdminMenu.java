package carmarket.ui;

import carmarket.enums.RequestStatus;
import carmarket.model.PurchaseRequest;
import carmarket.repository.CatalogRepository;
import carmarket.service.CarService;
import carmarket.service.DataExportService;
import carmarket.service.PurchaseRequestService;
import carmarket.service.StatisticsService;
import carmarket.service.SystemStatistics;
import carmarket.service.UserService;

import java.math.BigDecimal;
import java.nio.file.Path;

public class AdminMenu {

    private final UserService userService;
    private final CarService carService;
    private final PurchaseRequestService requestService;
    private final InputHelper inputHelper;
    private final StatisticsService statisticsService;
    private final DataExportService dataExportService;
    private final CatalogRepository catalogRepository;

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
        this.catalogRepository = new CatalogRepository();
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
                    case 10 -> {
                        ConsoleView.printMessage("Вы вышли из панели администратора.");
                        running = false;
                    }
                    default -> ConsoleView.printError("Такого пункта меню нет.");
                }
            } catch (RuntimeException e) {
                ConsoleView.printError(e.getMessage());
                inputHelper.pause();
            }
        }
    }

    private void printMenu() {
        ConsoleView.printFrameTitle("ПАНЕЛЬ АДМИНИСТРАТОРА");
        ConsoleView.printMenuOption(1, "Пользователи");
        ConsoleView.printMenuOption(2, "Автомобили");
        ConsoleView.printMenuOption(3, "Заявки");
        ConsoleView.printMenuOption(4, "Поиск");
        ConsoleView.printMenuOption(5, "Фильтрация");
        ConsoleView.printMenuOption(6, "Сортировка");
        ConsoleView.printMenuOption(7, "Статистика");
        ConsoleView.printMenuOption(8, "Экспорт в Excel");
        ConsoleView.printMenuOption(9, "Таблицы БД");
        ConsoleView.printMenuOption(10, "Выйти");
    }

    private void usersMenu() {
        boolean running = true;

        while (running) {
            ConsoleView.printSection("Пользователи");
            ConsoleView.printMenuOption(1, "Показать всех");
            ConsoleView.printMenuOption(2, "Найти по ID");
            ConsoleView.printMenuOption(3, "Назад");

            int choice = inputHelper.readInt("Выберите пункт: ");

            switch (choice) {
                case 1 -> {
                    ConsoleView.printUsers(userService.findAll());
                    inputHelper.pause();
                }
                case 2 -> {
                    long id = inputHelper.readPositiveLong("Введите ID пользователя: ");
                    ConsoleView.printUser(userService.findById(id));
                    inputHelper.pause();
                }
                case 3 -> running = false;
                default -> ConsoleView.printError("Такого пункта нет.");
            }
        }
    }

    private void carsMenu() {
        boolean running = true;

        while (running) {
            ConsoleView.printSection("Автомобили");
            ConsoleView.printMenuOption(1, "Показать все");
            ConsoleView.printMenuOption(2, "Найти по ID");
            ConsoleView.printMenuOption(3, "Назад");

            int choice = inputHelper.readInt("Выберите пункт: ");

            switch (choice) {
                case 1 -> {
                    ConsoleView.printCars(carService.findAll());
                    inputHelper.pause();
                }
                case 2 -> {
                    long id = inputHelper.readPositiveLong("Введите ID автомобиля: ");
                    ConsoleView.printCar(carService.findById(id));
                    inputHelper.pause();
                }
                case 3 -> running = false;
                default -> ConsoleView.printError("Такого пункта нет.");
            }
        }
    }

    private void requestsMenu() {
        boolean running = true;

        while (running) {
            ConsoleView.printSection("Заявки");
            ConsoleView.printMenuOption(1, "Показать все");
            ConsoleView.printMenuOption(2, "Найти по ID");
            ConsoleView.printMenuOption(3, "Изменить статус");
            ConsoleView.printMenuOption(4, "Назад");

            int choice = inputHelper.readInt("Выберите пункт: ");

            switch (choice) {
                case 1 -> {
                    ConsoleView.printRequests(requestService.findAll());
                    inputHelper.pause();
                }
                case 2 -> {
                    long id = inputHelper.readPositiveLong("Введите ID заявки: ");
                    ConsoleView.printRequest(requestService.findById(id));
                    inputHelper.pause();
                }
                case 3 -> changeRequestStatus();
                case 4 -> running = false;
                default -> ConsoleView.printError("Такого пункта нет.");
            }
        }
    }

    private void changeRequestStatus() {
        long id = inputHelper.readPositiveLong("Введите ID заявки: ");
        RequestStatus status = readRequestStatus();

        PurchaseRequest request = requestService.findById(id);
        request.setStatus(status);
        requestService.update(request);

        ConsoleView.printSuccess("Статус заявки изменён.");
        ConsoleView.printRequest(request);
        inputHelper.pause();
    }

    private void searchMenu() {
        ConsoleView.printSection("Поиск");
        ConsoleView.printMenuOption(1, "Поиск автомобиля по марке/модели");
        ConsoleView.printMenuOption(2, "Поиск заявок по имени пользователя");

        int choice = inputHelper.readInt("Выберите пункт: ");

        if (choice == 1) {
            String query = inputHelper.readString("Введите марку или модель: ");
            ConsoleView.printCars(carService.search(query));
            inputHelper.pause();
        } else if (choice == 2) {
            String query = inputHelper.readString("Введите имя пользователя: ");
            ConsoleView.printRequests(requestService.searchByUserName(query));
            inputHelper.pause();
        } else {
            ConsoleView.printError("Такого пункта нет.");
        }
    }

    private void filterMenu() {
        ConsoleView.printSection("Фильтрация");
        ConsoleView.printMenuOption(1, "Заявки по статусу");
        ConsoleView.printMenuOption(2, "Автомобили по диапазону цены");

        int choice = inputHelper.readInt("Выберите пункт: ");

        if (choice == 1) {
            RequestStatus status = readRequestStatus();
            ConsoleView.printRequests(requestService.filterByStatus(status));
            inputHelper.pause();
        } else if (choice == 2) {
            BigDecimal min =
                    inputHelper.readNonNegativePrice("Минимальная цена: ");
            BigDecimal max =
                    inputHelper.readNonNegativePrice("Максимальная цена: ");

            ConsoleView.printCars(carService.filterByPrice(min, max));
            inputHelper.pause();
        } else {
            ConsoleView.printError("Такого пункта нет.");
        }
    }

    private void sortMenu() {
        ConsoleView.printSection("Сортировка");
        ConsoleView.printMenuOption(1, "По цене");
        ConsoleView.printMenuOption(2, "По пробегу");

        int choice = inputHelper.readInt("Выберите пункт: ");

        ConsoleView.printMenuOption(1, "По возрастанию");
        ConsoleView.printMenuOption(2, "По убыванию");

        int order = inputHelper.readInt("Выберите порядок: ");

        if (order != 1 && order != 2) {
            ConsoleView.printError("Такого порядка сортировки нет.");
            return;
        }

        boolean ascending = order == 1;

        if (choice == 1) {
            ConsoleView.printCars(carService.sortByPrice(ascending));
            inputHelper.pause();
        } else if (choice == 2) {
            ConsoleView.printCars(carService.sortByMileage(ascending));
            inputHelper.pause();
        } else {
            ConsoleView.printError("Такого пункта нет.");
        }
    }

    private void databaseTablesMenu() {
        boolean running = true;

        while (running) {
            ConsoleView.printSection("Таблицы БД (3НФ)");
            ConsoleView.printMenuOption(1, "roles");
            ConsoleView.printMenuOption(2, "users");
            ConsoleView.printMenuOption(3, "brands");
            ConsoleView.printMenuOption(4, "car_models");
            ConsoleView.printMenuOption(5, "body_types");
            ConsoleView.printMenuOption(6, "transmissions");
            ConsoleView.printMenuOption(7, "fuel_types");
            ConsoleView.printMenuOption(8, "cars");
            ConsoleView.printMenuOption(9, "car_specifications");
            ConsoleView.printMenuOption(10, "purchase_requests");
            ConsoleView.printMenuOption(11, "Назад");

            int choice = inputHelper.readInt("Выберите пункт: ");

            switch (choice) {
                case 1 -> {
                    ConsoleView.printRoles(catalogRepository.findAllRoles());
                    inputHelper.pause();
                }
                case 2 -> {
                    ConsoleView.printUsers(userService.findAll());
                    inputHelper.pause();
                }
                case 3 -> {
                    ConsoleView.printBrands(catalogRepository.findAllBrands());
                    inputHelper.pause();
                }
                case 4 -> {
                    ConsoleView.printCarModels(catalogRepository.findAllModels());
                    inputHelper.pause();
                }
                case 5 -> {
                    ConsoleView.printCatalogItems(
                            catalogRepository.findAllBodyTypes(),
                            "Типы кузова не найдены."
                    );
                    inputHelper.pause();
                }
                case 6 -> {
                    ConsoleView.printCatalogItems(
                            catalogRepository.findAllTransmissions(),
                            "Коробки передач не найдены."
                    );
                    inputHelper.pause();
                }
                case 7 -> {
                    ConsoleView.printCatalogItems(
                            catalogRepository.findAllFuelTypes(),
                            "Типы топлива не найдены."
                    );
                    inputHelper.pause();
                }
                case 8 -> {
                    ConsoleView.printCars(carService.findAll());
                    inputHelper.pause();
                }
                case 9 -> {
                    ConsoleView.printSpecifications(catalogRepository.findAllSpecifications());
                    inputHelper.pause();
                }
                case 10 -> {
                    ConsoleView.printRequests(requestService.findAll());
                    inputHelper.pause();
                }
                case 11 -> running = false;
                default -> ConsoleView.printError("Такого пункта нет.");
            }
        }
    }

    private void statisticsMenu() {
        SystemStatistics statistics = statisticsService.calculate(
                userService.findAll(),
                carService.findAll(),
                requestService.findAll()
        );

        ConsoleView.printStatistics(statistics);
        inputHelper.pause();
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
        ConsoleView.printSuccess("Данные экспортированы в файл: "
                + Path.of(fileName).toAbsolutePath());
        inputHelper.pause();
    }

    private RequestStatus readRequestStatus() {
        ConsoleView.printRequestStatusMenu();

        while (true) {
            String value = inputHelper.readString(
                    "Введите статус (NEW, IN_PROGRESS, APPROVED, REJECTED, CANCELLED): "
            );

            try {
                return RequestStatus.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                ConsoleView.printError("Неизвестный статус. Попробуйте ещё раз.");
            }
        }
    }
}
