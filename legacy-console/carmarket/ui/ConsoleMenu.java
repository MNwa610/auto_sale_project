package carmarket.ui;

import carmarket.enums.UserRole;
import carmarket.model.User;
import carmarket.service.CarService;
import carmarket.service.PurchaseRequestService;
import carmarket.service.UserService;

import java.util.Scanner;

public class ConsoleMenu {

    private final UserService userService;
    private final InputHelper inputHelper;
    private final UserMenu userMenu;
    private final AdminMenu adminMenu;

    public ConsoleMenu() {
        Scanner scanner = new Scanner(System.in);

        UserService userService = new UserService();
        CarService carService = new CarService();
        PurchaseRequestService requestService = new PurchaseRequestService();

        this.userService = userService;
        this.inputHelper = new InputHelper(scanner);
        this.userMenu = new UserMenu(carService, requestService, inputHelper);
        this.adminMenu = new AdminMenu(
                userService,
                carService,
                requestService,
                inputHelper
        );
    }

    public void start() {
        boolean running = true;

        while (running) {
            printStartMenu();
            int choice = inputHelper.readInt("Выберите пункт: ");

            try {
                switch (choice) {
                    case 1 -> login();
                    case 2 -> register();
                    case 3 -> {
                        running = false;
                        ConsoleView.printSuccess("Работа программы завершена.");
                    }
                    default -> ConsoleView.printError("Такого пункта меню нет.");
                }
            } catch (RuntimeException e) {
                ConsoleView.printError(e.getMessage());
            }
        }
    }

    private void printStartMenu() {
        ConsoleView.printFrameTitle("МАРКЕТПЛЕЙС АВТОМОБИЛЕЙ С ПРОБЕГОМ");
        ConsoleView.printMenuOption(1, "Войти");
        ConsoleView.printMenuOption(2, "Зарегистрироваться");
        ConsoleView.printMenuOption(3, "Выход");
    }

    private void login() {
        ConsoleView.printSection("Вход в систему");
        String login = inputHelper.readString("  Логин: ");
        String password = inputHelper.readString("  Пароль: ");

        User user = userService.authenticate(login, password);

        ConsoleView.printSuccess("Добро пожаловать, " + user.getFullName() + "!");

        if (user.getRole() == UserRole.ADMIN) {
            adminMenu.show();
        } else {
            userMenu.show(user);
        }
    }

    private void register() {
        ConsoleView.printSection("Регистрация");
        String fullName = inputHelper.readString("  ФИО: ");
        String login = inputHelper.readString("  Логин: ");
        String password = inputHelper.readString("  Пароль: ");

        User user = userService.register(fullName, login, password);

        ConsoleView.printSuccess("Регистрация выполнена.");
        ConsoleView.printMessage("ID пользователя: " + user.getId());
        inputHelper.pause();
    }
}
