package carmarket.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    static {
        loadSettings();
    }

    private DatabaseManager() {
    }

    private static void loadSettings() {
        Properties properties = new Properties();

        try (InputStream input = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл database.properties не найден");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения database.properties", e);
        }

        dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = properties.getProperty("db.url");
        }

        dbUser = System.getenv("DB_USER");
        if (dbUser == null || dbUser.isBlank()) {
            dbUser = properties.getProperty("db.user");
        }

        dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null) {
            dbPassword = properties.getProperty("db.password");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
