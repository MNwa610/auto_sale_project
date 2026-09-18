package carmarket.ui;

import java.util.Scanner;

public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readString(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public int readInt(String message) {
        System.out.print(message);
        return Integer.parseInt(scanner.nextLine().trim());
    }

    public long readLong(String message) {
        System.out.print(message);
        return Long.parseLong(scanner.nextLine().trim());
    }

    public double readDouble(String message) {
        System.out.print(message);
        return Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));
    }
}
