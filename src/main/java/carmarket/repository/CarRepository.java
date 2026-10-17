package carmarket.repository;

import carmarket.enums.CarStatus;
import carmarket.exception.BusinessException;
import carmarket.exception.DatabaseException;
import carmarket.model.Car;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarRepository {

    private static final String CAR_SELECT =
            "SELECT c.id, c.seller_id, c.year, c.mileage, c.price, c.status, c.created_at, " +
                    "b.name AS brand, m.name AS model, u.full_name AS seller_full_name, " +
                    "s.vin, bt.name AS body_type, t.name AS transmission, " +
                    "f.name AS fuel_type, s.engine_volume, s.description " +
                    "FROM cars c " +
                    "JOIN car_models m ON m.id = c.model_id " +
                    "JOIN brands b ON b.id = m.brand_id " +
                    "JOIN users u ON u.id = c.seller_id " +
                    "JOIN car_specifications s ON s.car_id = c.id " +
                    "JOIN body_types bt ON bt.id = s.body_type_id " +
                    "JOIN transmissions t ON t.id = s.transmission_id " +
                    "JOIN fuel_types f ON f.id = s.fuel_type_id";

    public Car create(Car car) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long brandId = findCatalogId(connection, "brands", car.getBrand());
                long modelId = findModelId(connection, brandId, car.getModel());
                long bodyTypeId = findCatalogId(connection, "body_types", car.getBodyType());
                long transmissionId = findCatalogId(connection, "transmissions", car.getTransmission());
                long fuelTypeId = findCatalogId(connection, "fuel_types", car.getFuelType());

                String sql = "INSERT INTO cars (seller_id, model_id, year, mileage, price, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at";
                long id;
                LocalDateTime createdAt;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, car.getSellerId());
                    statement.setLong(2, modelId);
                    statement.setInt(3, car.getYear());
                    statement.setInt(4, car.getMileage());
                    statement.setBigDecimal(5, car.getPrice());
                    statement.setString(6, car.getStatus().name());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        id = resultSet.getLong("id");
                        createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
                    }
                }
                insertSpecification(connection, car, id, bodyTypeId, transmissionId, fuelTypeId);
                connection.commit();
                car.setId(id);
                car.setCreatedAt(createdAt);
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            checkVinConflict(e);
            throw new DatabaseException("Ошибка при создании автомобиля", e);
        }

        return car;
    }

    public List<Car> findAll() {
        return findList(CAR_SELECT + " ORDER BY c.id", null);
    }

    public List<Car> findBySellerId(Long sellerId) {
        return findList(CAR_SELECT + " WHERE c.seller_id = ? ORDER BY c.id", sellerId);
    }

    private List<Car> findList(String sql, Long sellerId) {
        List<Car> cars = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (sellerId != null) {
                statement.setLong(1, sellerId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cars.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка автомобилей", e);
        }

        return cars;
    }

    public Car findById(Long id) {
        String sql = CAR_SELECT + " WHERE c.id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске автомобиля по id", e);
        }

        return null;
    }

    public Car findByVin(String vin) {
        String sql = CAR_SELECT + " WHERE UPPER(s.vin) = UPPER(?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vin);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при проверке VIN", e);
        }
    }

    public boolean update(Car car) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long brandId = findCatalogId(connection, "brands", car.getBrand());
                long modelId = findModelId(connection, brandId, car.getModel());
                long bodyTypeId = findCatalogId(connection, "body_types", car.getBodyType());
                long transmissionId = findCatalogId(connection, "transmissions", car.getTransmission());
                long fuelTypeId = findCatalogId(connection, "fuel_types", car.getFuelType());

                String carSql = "UPDATE cars SET seller_id = ?, model_id = ?, year = ?, mileage = ?, " +
                        "price = ?, status = ? WHERE id = ?";

                int updated;
                try (PreparedStatement statement = connection.prepareStatement(carSql)) {
                    statement.setLong(1, car.getSellerId());
                    statement.setLong(2, modelId);
                    statement.setInt(3, car.getYear());
                    statement.setInt(4, car.getMileage());
                    statement.setBigDecimal(5, car.getPrice());
                    statement.setString(6, car.getStatus().name());
                    statement.setLong(7, car.getId());
                    updated = statement.executeUpdate();
                }

                if (updated > 0) {
                    updateSpecification(connection, car, bodyTypeId, transmissionId, fuelTypeId);
                }
                connection.commit();
                return updated > 0;
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            checkVinConflict(e);
            throw new DatabaseException("Ошибка при обновлении автомобиля", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM cars WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new BusinessException("Нельзя удалить автомобиль: на него есть заявки. Обновите таблицу.");
            }
            throw new DatabaseException("Ошибка при удалении автомобиля", e);
        }
    }

    private void insertSpecification(Connection connection, Car car, long carId,
                                     long bodyTypeId, long transmissionId, long fuelTypeId)
            throws SQLException {
        String sql = "INSERT INTO car_specifications " +
                "(car_id, vin, body_type_id, transmission_id, fuel_type_id, engine_volume, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, carId);
            bindSpecification(statement, car, bodyTypeId, transmissionId, fuelTypeId, true);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Характеристики автомобиля не найдены");
            }
        }
    }

    private void updateSpecification(Connection connection, Car car,
                                     long bodyTypeId, long transmissionId, long fuelTypeId)
            throws SQLException {
        String sql = "UPDATE car_specifications SET vin = ?, body_type_id = ?, transmission_id = ?, " +
                "fuel_type_id = ?, engine_volume = ?, description = ? WHERE car_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSpecification(statement, car, bodyTypeId, transmissionId, fuelTypeId, false);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Характеристики автомобиля не найдены");
            }
        }
    }

    private void bindSpecification(PreparedStatement statement, Car car,
                                   long bodyTypeId, long transmissionId, long fuelTypeId,
                                   boolean insert) throws SQLException {
        int index = 1;
        if (insert) {
            index++;
        }

        if (car.getVin() != null) {
            statement.setString(index++, car.getVin());
        } else {
            statement.setNull(index++, Types.VARCHAR);
        }

        statement.setLong(index++, bodyTypeId);
        statement.setLong(index++, transmissionId);
        statement.setLong(index++, fuelTypeId);

        if (car.getEngineVolume() != null) {
            statement.setBigDecimal(index++, car.getEngineVolume());
        } else {
            statement.setNull(index++, Types.NUMERIC);
        }

        statement.setString(index++, car.getDescription());

        if (!insert) {
            statement.setLong(index, car.getId());
        }
    }

    private long findCatalogId(Connection connection, String table, String name)
            throws SQLException {
        String selectSql = "SELECT id FROM " + table + " WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }

        throw new BusinessException("Значение справочника больше недоступно. Откройте форму заново.");
    }

    private long findModelId(Connection connection, long brandId, String name)
            throws SQLException {
        String selectSql = "SELECT id FROM car_models WHERE brand_id = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setLong(1, brandId);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }

        throw new BusinessException("Модель больше недоступна. Выберите модель из справочника.");
    }

    private Car mapRow(ResultSet resultSet) throws SQLException {
        Car car = new Car();
        car.setId(resultSet.getLong("id"));
        car.setSellerId(resultSet.getLong("seller_id"));
        car.setSellerFullName(resultSet.getString("seller_full_name"));
        car.setBrand(resultSet.getString("brand"));
        car.setModel(resultSet.getString("model"));
        car.setYear(resultSet.getInt("year"));
        car.setMileage(resultSet.getInt("mileage"));
        car.setPrice(resultSet.getBigDecimal("price"));
        car.setVin(resultSet.getString("vin"));
        car.setBodyType(resultSet.getString("body_type"));
        car.setTransmission(resultSet.getString("transmission"));
        car.setFuelType(resultSet.getString("fuel_type"));
        car.setEngineVolume(resultSet.getBigDecimal("engine_volume"));
        car.setDescription(resultSet.getString("description"));
        car.setStatus(CarStatus.valueOf(resultSet.getString("status")));

        if (resultSet.getTimestamp("created_at") != null) {
            car.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        return car;
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private void checkVinConflict(SQLException e) {
        if ("23505".equals(e.getSQLState())) {
            throw new BusinessException("Поле «VIN»: автомобиль с таким VIN уже существует. Укажите другой VIN.");
        }
    }
}
