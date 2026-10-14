package carmarket.util;

import carmarket.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String loadError;

    static {
        loadSettings();
    }

    private DatabaseManager() {
    }

    private static void loadSettings() {
        Properties properties = new Properties();

        try (InputStream input = DatabaseManager.class.getResourceAsStream("/database.properties")) {
            if (input == null) {
                loadError = "Файл database.properties не найден. Скопируйте database.properties.example.";
                log.error(loadError);
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            loadError = "Не удалось прочитать database.properties";
            log.error(loadError, e);
            return;
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

        if (dbUrl == null || dbUrl.isBlank() || dbUser == null || dbUser.isBlank()) {
            loadError = "В database.properties не заданы db.url или db.user";
            log.error(loadError);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (loadError != null) {
            throw new DatabaseException(loadError);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static void checkConnection() {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new DatabaseException("База данных не ответила на проверку подключения");
            }
            log.info("Подключение к PostgreSQL проверено");
        } catch (SQLException e) {
            log.error("Не удалось подключиться к PostgreSQL", e);
            throw new DatabaseException("Не удалось подключиться к PostgreSQL", e);
        }
    }
}
