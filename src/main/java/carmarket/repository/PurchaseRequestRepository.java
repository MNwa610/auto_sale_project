package carmarket.repository;

import carmarket.enums.RequestStatus;
import carmarket.model.PurchaseRequest;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PurchaseRequestRepository {

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
            throw new RuntimeException("Ошибка при получении списка заявок", e);
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
            throw new RuntimeException("Ошибка при поиске заявки по id", e);
        }

        return null;
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
