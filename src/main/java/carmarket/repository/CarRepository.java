package carmarket.repository;

import carmarket.enums.CarStatus;
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

    public Car create(Car car) {
        String sql = "INSERT INTO cars (seller_id, brand, model, year, mileage, price, vin, " +
                "body_type, transmission, fuel_type, engine_volume, description, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, created_at";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, car.getSellerId());
            statement.setString(2, car.getBrand());
            statement.setString(3, car.getModel());
            statement.setInt(4, car.getYear());
            statement.setInt(5, car.getMileage());
            statement.setBigDecimal(6, car.getPrice());

            if (car.getVin() != null) {
                statement.setString(7, car.getVin());
            } else {
                statement.setNull(7, Types.VARCHAR);
            }

            statement.setString(8, car.getBodyType());
            statement.setString(9, car.getTransmission());
            statement.setString(10, car.getFuelType());
            statement.setBigDecimal(11, car.getEngineVolume());
            statement.setString(12, car.getDescription());
            statement.setString(13, car.getStatus().name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    car.setId(resultSet.getLong("id"));
                    if (resultSet.getTimestamp("created_at") != null) {
                        car.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при создании автомобиля", e);
        }

        return car;
    }

    public List<Car> findAll() {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT id, seller_id, brand, model, year, mileage, price, vin, " +
                "body_type, transmission, fuel_type, engine_volume, description, status, created_at " +
                "FROM cars ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                cars.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка автомобилей", e);
        }

        return cars;
    }

    public Car findById(Long id) {
        String sql = "SELECT id, seller_id, brand, model, year, mileage, price, vin, " +
                "body_type, transmission, fuel_type, engine_volume, description, status, created_at " +
                "FROM cars WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске автомобиля по id", e);
        }

        return null;
    }

    public boolean update(Car car) {
        String sql = "UPDATE cars SET seller_id = ?, brand = ?, model = ?, year = ?, mileage = ?, " +
                "price = ?, vin = ?, body_type = ?, transmission = ?, fuel_type = ?, " +
                "engine_volume = ?, description = ?, status = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, car.getSellerId());
            statement.setString(2, car.getBrand());
            statement.setString(3, car.getModel());
            statement.setInt(4, car.getYear());
            statement.setInt(5, car.getMileage());
            statement.setBigDecimal(6, car.getPrice());

            if (car.getVin() != null) {
                statement.setString(7, car.getVin());
            } else {
                statement.setNull(7, Types.VARCHAR);
            }

            statement.setString(8, car.getBodyType());
            statement.setString(9, car.getTransmission());
            statement.setString(10, car.getFuelType());
            statement.setBigDecimal(11, car.getEngineVolume());
            statement.setString(12, car.getDescription());
            statement.setString(13, car.getStatus().name());
            statement.setLong(14, car.getId());

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении автомобиля", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM cars WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении автомобиля", e);
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
