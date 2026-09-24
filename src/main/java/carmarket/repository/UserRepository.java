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

    private static final String USER_SELECT =
            "SELECT u.id, u.full_name, u.created_at, r.code AS role, " +
                    "c.login, c.password_hash " +
                    "FROM users u " +
                    "JOIN roles r ON r.id = u.role_id " +
                    "JOIN user_credentials c ON c.user_id = u.id";

    public User create(User user) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long roleId = findRoleId(connection, user.getRole().name());

                String userSql = "INSERT INTO users (full_name, role_id) VALUES (?, ?) RETURNING id, created_at";
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setString(1, user.getFullName());
                    statement.setLong(2, roleId);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            user.setId(resultSet.getLong("id"));
                            if (resultSet.getTimestamp("created_at") != null) {
                                user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                            }
                        }
                    }
                }

                String credentialsSql = "INSERT INTO user_credentials (user_id, login, password_hash) VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(credentialsSql)) {
                    statement.setLong(1, user.getId());
                    statement.setString(2, user.getLogin());
                    statement.setString(3, user.getPasswordHash());
                    statement.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при создании пользователя", e);
        }

        return user;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = USER_SELECT + " ORDER BY u.id";

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
        String sql = USER_SELECT + " WHERE u.id = ?";

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
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long roleId = findRoleId(connection, user.getRole().name());

                String userSql = "UPDATE users SET full_name = ?, role_id = ? WHERE id = ?";
                int updated;
                try (PreparedStatement statement = connection.prepareStatement(userSql)) {
                    statement.setString(1, user.getFullName());
                    statement.setLong(2, roleId);
                    statement.setLong(3, user.getId());
                    updated = statement.executeUpdate();
                }

                if (updated > 0) {
                    String credentialsSql = "UPDATE user_credentials SET login = ?, password_hash = ? WHERE user_id = ?";
                    try (PreparedStatement statement = connection.prepareStatement(credentialsSql)) {
                        statement.setString(1, user.getLogin());
                        statement.setString(2, user.getPasswordHash());
                        statement.setLong(3, user.getId());
                        statement.executeUpdate();
                    }
                }

                connection.commit();
                return updated > 0;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
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

    private long findRoleId(Connection connection, String code) throws SQLException {
        String sql = "SELECT id FROM roles WHERE code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }
        throw new DatabaseException("Роль " + code + " не найдена в справочнике");
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
