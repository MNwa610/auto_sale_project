package carmarket.ui;

import java.math.BigDecimal;
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

    public int readNonNegativeInt(String message) {
        while (true) {
            int value = readInt(message);

            if (value >= 0) {
                return value;
            }

            System.out.println("Ошибка: значение не может быть отрицательным.");
        }
    }

    public int readMileage(String message) {
        while (true) {
            int mileage = readInt(message);

            if (mileage >= 0) {
                return mileage;
            }

            System.out.println("Ошибка: пробег не может быть отрицательным.");
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

    public long readPositiveLong(String message) {
        while (true) {
            long value = readLong(message);

            if (value > 0) {
                return value;
            }

            System.out.println("Ошибка: ID должен быть положительным числом.");
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

    public BigDecimal readNonNegativePrice(String message) {
        while (true) {
            double value = readDouble(message);

            if (value >= 0) {
                return BigDecimal.valueOf(value);
            }

            System.out.println("Ошибка: цена не может быть отрицательной.");
        }
    }

    public BigDecimal readPositivePrice(String message) {
        while (true) {
            double value = readDouble(message);

            if (value > 0) {
                return BigDecimal.valueOf(value);
            }

            System.out.println("Ошибка: цена должна быть больше 0.");
        }
    }

    public void pause() {
        System.out.print("Нажмите Enter, чтобы продолжить...");
        scanner.nextLine();
        System.out.println();
    }
}
