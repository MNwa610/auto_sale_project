package carmarket.ui;

import carmarket.enums.RequestStatus;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.service.CarService;
import carmarket.service.PurchaseRequestService;

import java.util.List;

public class UserMenu {

    private final CarService carService;
    private final PurchaseRequestService requestService;
    private final InputHelper inputHelper;

    public UserMenu(CarService carService,
                    PurchaseRequestService requestService,
                    InputHelper inputHelper) {
        this.carService = carService;
        this.requestService = requestService;
        this.inputHelper = inputHelper;
    }

    public void show(User currentUser) {
        boolean running = true;

        while (running) {
            printMenu(currentUser);
            int choice = inputHelper.readInt("Выберите пункт: ");

            try {
                switch (choice) {
                    case 1 -> {
                        ConsoleView.printCars(carService.findAll());
                        inputHelper.pause();
                    }
                    case 2 -> searchCar();
                    case 3 -> showCarById();
                    case 4 -> createRequest(currentUser);
                    case 5 -> showMyRequests(currentUser);
                    case 6 -> {
                        ConsoleView.printUser(currentUser);
                        inputHelper.pause();
                    }
                    case 7 -> {
                        ConsoleView.printMessage("Вы вышли из аккаунта.");
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

    private void printMenu(User currentUser) {
        ConsoleView.printFrameTitle("МЕНЮ ПОЛЬЗОВАТЕЛЯ");
        ConsoleView.printMessage("Вы вошли как: " + currentUser.getFullName());
        ConsoleView.printMenuOption(1, "Просмотреть автомобили");
        ConsoleView.printMenuOption(2, "Найти автомобиль");
        ConsoleView.printMenuOption(3, "Просмотреть автомобиль по ID");
        ConsoleView.printMenuOption(4, "Подать заявку на покупку");
        ConsoleView.printMenuOption(5, "Мои заявки");
        ConsoleView.printMenuOption(6, "Мои данные");
        ConsoleView.printMenuOption(7, "Выйти из аккаунта");
    }

    private void searchCar() {
        String query = inputHelper.readString("Введите марку или модель: ");
        ConsoleView.printCars(carService.search(query));
        inputHelper.pause();
    }

    private void showCarById() {
        long id = inputHelper.readPositiveLong("Введите ID автомобиля: ");
        ConsoleView.printCar(carService.findById(id));
        inputHelper.pause();
    }

    private void createRequest(User currentUser) {
        long carId = inputHelper.readPositiveLong("Введите ID автомобиля: ");
        String message = inputHelper.readOptionalString("Сообщение продавцу (необязательно): ");

        PurchaseRequest request = new PurchaseRequest(
                currentUser.getId(),
                carId,
                message,
                RequestStatus.NEW
        );

        PurchaseRequest created = requestService.create(request);

        ConsoleView.printSuccess("Заявка создана.");
        ConsoleView.printRequest(created);
        inputHelper.pause();
    }

    private void showMyRequests(User currentUser) {
        List<PurchaseRequest> requests =
                requestService.findByUserId(currentUser.getId());

        ConsoleView.printRequests(requests);
        inputHelper.pause();
    }
}
