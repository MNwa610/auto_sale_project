package carmarket.repository;

import carmarket.enums.CarStatus;
import carmarket.exception.DatabaseException;
import carmarket.model.Car;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CarRepository {

    private static final String CAR_SELECT =
            "SELECT c.id, c.seller_id, c.year, c.mileage, c.price, c.status, c.created_at, " +
                    "b.name AS brand, m.name AS model, " +
                    "s.vin, bt.name AS body_type, t.name AS transmission, " +
                    "f.name AS fuel_type, s.engine_volume, s.description " +
                    "FROM cars c " +
                    "JOIN car_models m ON m.id = c.model_id " +
                    "JOIN brands b ON b.id = m.brand_id " +
                    "JOIN car_specifications s ON s.car_id = c.id " +
                    "JOIN body_types bt ON bt.id = s.body_type_id " +
                    "JOIN transmissions t ON t.id = s.transmission_id " +
                    "JOIN fuel_types f ON f.id = s.fuel_type_id";

    public Car create(Car car) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long brandId = findOrCreateByName(connection, "brands", car.getBrand());
                long modelId = findOrCreateModel(connection, brandId, car.getModel());
                long bodyTypeId = findOrCreateByName(connection, "body_types", car.getBodyType());
                long transmissionId = findOrCreateByName(connection, "transmissions", car.getTransmission());
                long fuelTypeId = findOrCreateByName(connection, "fuel_types", car.getFuelType());

                String carSql = "INSERT INTO cars (seller_id, model_id, year, mileage, price, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at";

                try (PreparedStatement statement = connection.prepareStatement(carSql)) {
                    statement.setLong(1, car.getSellerId());
                    statement.setLong(2, modelId);
                    statement.setInt(3, car.getYear());
                    statement.setInt(4, car.getMileage());
                    statement.setBigDecimal(5, car.getPrice());
                    statement.setString(6, car.getStatus().name());

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            car.setId(resultSet.getLong("id"));
                            if (resultSet.getTimestamp("created_at") != null) {
                                car.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                            }
                        }
                    }
                }

                insertSpecification(connection, car, bodyTypeId, transmissionId, fuelTypeId);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при создании автомобиля", e);
        }

        return car;
    }

    public List<Car> findAll() {
        List<Car> cars = new ArrayList<>();
        String sql = CAR_SELECT + " ORDER BY c.id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                cars.add(mapRow(resultSet));
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

    public boolean update(Car car) {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long brandId = findOrCreateByName(connection, "brands", car.getBrand());
                long modelId = findOrCreateModel(connection, brandId, car.getModel());
                long bodyTypeId = findOrCreateByName(connection, "body_types", car.getBodyType());
                long transmissionId = findOrCreateByName(connection, "transmissions", car.getTransmission());
                long fuelTypeId = findOrCreateByName(connection, "fuel_types", car.getFuelType());

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
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
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
            throw new DatabaseException("Ошибка при удалении автомобиля", e);
        }
    }

    private void insertSpecification(Connection connection, Car car,
                                     long bodyTypeId, long transmissionId, long fuelTypeId)
            throws SQLException {
        String sql = "INSERT INTO car_specifications " +
                "(car_id, vin, body_type_id, transmission_id, fuel_type_id, engine_volume, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSpecification(statement, car, bodyTypeId, transmissionId, fuelTypeId, true);
            statement.executeUpdate();
        }
    }

    private void updateSpecification(Connection connection, Car car,
                                     long bodyTypeId, long transmissionId, long fuelTypeId)
            throws SQLException {
        String sql = "UPDATE car_specifications SET vin = ?, body_type_id = ?, transmission_id = ?, " +
                "fuel_type_id = ?, engine_volume = ?, description = ? WHERE car_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSpecification(statement, car, bodyTypeId, transmissionId, fuelTypeId, false);
            statement.executeUpdate();
        }
    }

    private void bindSpecification(PreparedStatement statement, Car car,
                                   long bodyTypeId, long transmissionId, long fuelTypeId,
                                   boolean insert) throws SQLException {
        int index = 1;
        if (insert) {
            statement.setLong(index++, car.getId());
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

    private long findOrCreateByName(Connection connection, String table, String name)
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

        String insertSql = "INSERT INTO " + table + " (name) VALUES (?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private long findOrCreateModel(Connection connection, long brandId, String name)
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

        String insertSql = "INSERT INTO car_models (brand_id, name) VALUES (?, ?) RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setLong(1, brandId);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("id");
            }
        }
    }

    private Car mapRow(ResultSet resultSet) throws SQLException {
        Car car = new Car();
        car.setId(resultSet.getLong("id"));
        car.setSellerId(resultSet.getLong("seller_id"));
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
}
