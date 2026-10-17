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

    private static final String REQUEST_COLUMNS =
            "SELECT pr.id, pr.user_id, pr.car_id, pr.message, pr.status, pr.created_at, pr.updated_at";

    private static final String REQUEST_DETAILS_JOIN =
            " JOIN users u ON u.id = pr.user_id" +
                    " JOIN cars c ON c.id = pr.car_id" +
                    " JOIN car_models m ON m.id = c.model_id" +
                    " JOIN brands b ON b.id = m.brand_id";

    private static final String REQUEST_DETAILS_SELECT = REQUEST_COLUMNS
            + ", u.full_name AS user_full_name, b.name AS car_brand, m.name AS car_model"
            + ", c.price AS car_price, c.mileage AS car_mileage"
            + " FROM purchase_requests pr" + REQUEST_DETAILS_JOIN;

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
        return findList(REQUEST_COLUMNS + " FROM purchase_requests pr ORDER BY pr.id");
    }

    public List<PurchaseRequest> findAllWithDetails() {
        return findListWithDetails(REQUEST_DETAILS_SELECT + " ORDER BY pr.id", null);
    }

    public List<PurchaseRequest> findByUserId(Long userId) {
        return findListWithDetails(REQUEST_DETAILS_SELECT + " WHERE pr.user_id = ? ORDER BY pr.id", userId);
    }

    public List<PurchaseRequest> findByCarId(Long carId) {
        return findListWithDetails(REQUEST_DETAILS_SELECT + " WHERE pr.car_id = ? ORDER BY pr.id", carId);
    }

    public PurchaseRequest findById(Long id) {
        String sql = REQUEST_COLUMNS + " FROM purchase_requests pr WHERE pr.id = ?";

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

    private PurchaseRequest mapRowWithDetails(ResultSet resultSet) throws SQLException {
        PurchaseRequest request = mapRow(resultSet);
        request.setUserFullName(resultSet.getString("user_full_name"));
        request.setCarBrand(resultSet.getString("car_brand"));
        request.setCarModel(resultSet.getString("car_model"));
        request.setCarPrice(resultSet.getBigDecimal("car_price"));
        request.setCarMileage(resultSet.getInt("car_mileage"));
        return request;
    }

    private List<PurchaseRequest> findList(String sql) {
        return findList(sql, null);
    }

    private List<PurchaseRequest> findList(String sql, Long parameter) {
        List<PurchaseRequest> requests = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setLong(1, parameter);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка заявок", e);
        }

        return requests;
    }

    private List<PurchaseRequest> findListWithDetails(String sql, Long parameter) {
        List<PurchaseRequest> requests = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setLong(1, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRowWithDetails(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка заявок", e);
        }

        return requests;
    }
}
