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
                        System.out.println("Работа программы завершена.");
                    }
                    default -> System.out.println("Ошибка: такого пункта меню нет.");
                }
            } catch (RuntimeException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void printStartMenu() {
        System.out.println();
        System.out.println("===============================================");
        System.out.println("  МАРКЕТПЛЕЙС АВТОМОБИЛЕЙ С ПРОБЕГОМ");
        System.out.println("===============================================");
        System.out.println("1. Войти");
        System.out.println("2. Зарегистрироваться");
        System.out.println("3. Выход");
    }

    private void login() {
        String login = inputHelper.readString("Логин: ");
        String password = inputHelper.readString("Пароль: ");

        User user = userService.authenticate(login, password);

        System.out.println("Вход выполнен: " + user.getFullName());

        if (user.getRole() == UserRole.ADMIN) {
            adminMenu.show();
        } else {
            userMenu.show(user);
        }
    }

    private void register() {
        String fullName = inputHelper.readString("ФИО: ");
        String login = inputHelper.readString("Логин: ");
        String password = inputHelper.readString("Пароль: ");

        User user = userService.register(fullName, login, password);

        System.out.println("Регистрация выполнена.");
        System.out.println("ID пользователя: " + user.getId());
    }
}
