package carmarket.repository;

import carmarket.enums.RequestStatus;
import carmarket.exception.DatabaseException;
import carmarket.model.PurchaseRequest;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PurchaseRequestRepository {

    public PurchaseRequest create(PurchaseRequest request) {
        String sql = "INSERT INTO purchase_requests (user_id, car_id, message, status) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at, updated_at";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, request.getUserId());
            statement.setLong(2, request.getCarId());
            statement.setString(3, request.getMessage());
            statement.setString(4, request.getStatus().name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    request.setId(resultSet.getLong("id"));
                    if (resultSet.getTimestamp("created_at") != null) {
                        request.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                    }
                    if (resultSet.getTimestamp("updated_at") != null) {
                        request.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при создании заявки", e);
        }

        return request;
    }

    public List<PurchaseRequest> findAll() {
        List<PurchaseRequest> requests = new ArrayList<>();
        String sql = "SELECT id, user_id, car_id, message, status, created_at, updated_at " +
                "FROM purchase_requests ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                requests.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка заявок", e);
        }

        return requests;
    }

    public PurchaseRequest findById(Long id) {
        String sql = "SELECT id, user_id, car_id, message, status, created_at, updated_at " +
                "FROM purchase_requests WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске заявки по id", e);
        }

        return null;
    }

    public boolean update(PurchaseRequest request) {
        String sql = "UPDATE purchase_requests SET user_id = ?, car_id = ?, message = ?, " +
                "status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, request.getUserId());
            statement.setLong(2, request.getCarId());
            statement.setString(3, request.getMessage());
            statement.setString(4, request.getStatus().name());
            statement.setLong(5, request.getId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении заявки", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM purchase_requests WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении заявки", e);
        }
    }

    private PurchaseRequest mapRow(ResultSet resultSet) throws SQLException {
        PurchaseRequest request = new PurchaseRequest();
        request.setId(resultSet.getLong("id"));
        request.setUserId(resultSet.getLong("user_id"));
        request.setCarId(resultSet.getLong("car_id"));
        request.setMessage(resultSet.getString("message"));
        request.setStatus(RequestStatus.valueOf(resultSet.getString("status")));

        if (resultSet.getTimestamp("created_at") != null) {
            request.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            request.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return request;
    }
}
