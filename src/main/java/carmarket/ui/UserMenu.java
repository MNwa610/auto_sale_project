package carmarket.ui;

import carmarket.enums.RequestStatus;
import carmarket.model.Car;
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
            printMenu();
            int choice = inputHelper.readInt("Выберите пункт: ");

            try {
                switch (choice) {
                    case 1 -> printCars(carService.findAll());
                    case 2 -> searchCar();
                    case 3 -> showCarById();
                    case 4 -> createRequest(currentUser);
                    case 5 -> showMyRequests(currentUser);
                    case 6 -> System.out.println(currentUser);
                    case 7 -> running = false;
                    default -> System.out.println("Ошибка: такого пункта меню нет.");
                }
            } catch (RuntimeException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========== МЕНЮ ПОЛЬЗОВАТЕЛЯ ==========");
        System.out.println("1. Просмотреть автомобили");
        System.out.println("2. Найти автомобиль");
        System.out.println("3. Просмотреть автомобиль по ID");
        System.out.println("4. Подать заявку");
        System.out.println("5. Мои заявки");
        System.out.println("6. Мои данные");
        System.out.println("7. Выйти");
    }

    private void searchCar() {
        String query = inputHelper.readString("Введите марку или модель: ");
        printCars(carService.search(query));
    }

    private void showCarById() {
        long id = inputHelper.readPositiveLong("Введите ID автомобиля: ");
        System.out.println(carService.findById(id));
    }

    private void createRequest(User currentUser) {
        long carId = inputHelper.readPositiveLong("Введите ID автомобиля: ");
        String message = inputHelper.readOptionalString("Сообщение продавцу: ");

        PurchaseRequest request = new PurchaseRequest(
                currentUser.getId(),
                carId,
                message,
                RequestStatus.NEW
        );

        PurchaseRequest created = requestService.create(request);

        System.out.println("Заявка создана. ID: " + created.getId());
    }

    private void showMyRequests(User currentUser) {
        List<PurchaseRequest> requests =
                requestService.findByUserId(currentUser.getId());

        if (requests.isEmpty()) {
            System.out.println("У вас пока нет заявок.");
            return;
        }

        for (PurchaseRequest request : requests) {
            System.out.println(request);
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
}
