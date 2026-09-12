package carmarket.repository;

import carmarket.enums.UserRole;
import carmarket.exception.DatabaseException;
import carmarket.model.User;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public User create(User user) {
        String sql = "INSERT INTO users (full_name, login, password_hash, role) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole().name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    user.setId(resultSet.getLong("id"));
                    if (resultSet.getTimestamp("created_at") != null) {
                        user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при создании пользователя", e);
        }

        return user;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, full_name, login, password_hash, role, created_at " +
                "FROM users ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка пользователей", e);
        }

        return users;
    }

    public User findById(Long id) {
        String sql = "SELECT id, full_name, login, password_hash, role, created_at " +
                "FROM users WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске пользователя по id", e);
        }

        return null;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, login = ?, password_hash = ?, role = ? " +
                "WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole().name());
            statement.setLong(5, user.getId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении пользователя", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении пользователя", e);
        }
    }

    private User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setFullName(resultSet.getString("full_name"));
        user.setLogin(resultSet.getString("login"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(UserRole.valueOf(resultSet.getString("role")));

        if (resultSet.getTimestamp("created_at") != null) {
            user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        return user;
    }
}
