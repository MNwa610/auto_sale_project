package carmarket.repository;

import carmarket.enums.UserRole;
import carmarket.model.User;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

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
            throw new RuntimeException("Ошибка при получении списка пользователей", e);
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
            throw new RuntimeException("Ошибка при поиске пользователя по id", e);
        }

        return null;
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
