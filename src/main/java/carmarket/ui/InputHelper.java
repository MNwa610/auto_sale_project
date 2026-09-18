package carmarket.ui;

import java.util.Scanner;

public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readString(String message) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim();

            if (!value.isEmpty()) {
                return value;
            }

            System.out.println("Ошибка: строка не может быть пустой.");
        }
    }

    public String readOptionalString(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public int readInt(String message) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim();

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: нужно ввести целое число.");
            }
        }
    }

    public long readLong(String message) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim();

            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    public double readDouble(String message) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim().replace(',', '.');

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: нужно ввести число.");
            }
        }
    }
}
