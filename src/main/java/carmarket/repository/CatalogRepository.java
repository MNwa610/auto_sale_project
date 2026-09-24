package carmarket.repository;

import carmarket.exception.DatabaseException;
import carmarket.model.Brand;
import carmarket.model.CarModel;
import carmarket.model.CarSpecification;
import carmarket.model.NamedCatalogItem;
import carmarket.model.Role;
import carmarket.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CatalogRepository {

    public List<Role> findAllRoles() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT id, code, name FROM roles ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Role role = new Role();
                role.setId(resultSet.getLong("id"));
                role.setCode(resultSet.getString("code"));
                role.setName(resultSet.getString("name"));
                roles.add(role);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении ролей", e);
        }

        return roles;
    }

    public List<Brand> findAllBrands() {
        List<Brand> brands = new ArrayList<>();
        String sql = "SELECT id, name FROM brands ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                brands.add(new Brand(resultSet.getLong("id"), resultSet.getString("name")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении марок", e);
        }

        return brands;
    }

    public List<CarModel> findAllModels() {
        List<CarModel> models = new ArrayList<>();
        String sql = "SELECT m.id, m.brand_id, b.name AS brand_name, m.name " +
                "FROM car_models m JOIN brands b ON b.id = m.brand_id ORDER BY m.id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                CarModel model = new CarModel();
                model.setId(resultSet.getLong("id"));
                model.setBrandId(resultSet.getLong("brand_id"));
                model.setBrandName(resultSet.getString("brand_name"));
                model.setName(resultSet.getString("name"));
                models.add(model);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении моделей", e);
        }

        return models;
    }

    public List<NamedCatalogItem> findAllBodyTypes() {
        return findNamedItems("body_types", "типов кузова");
    }

    public List<NamedCatalogItem> findAllTransmissions() {
        return findNamedItems("transmissions", "коробок передач");
    }

    public List<NamedCatalogItem> findAllFuelTypes() {
        return findNamedItems("fuel_types", "типов топлива");
    }

    public List<CarSpecification> findAllSpecifications() {
        List<CarSpecification> specifications = new ArrayList<>();
        String sql = "SELECT s.car_id, s.vin, s.engine_volume, s.description, " +
                "bt.name AS body_type, t.name AS transmission, f.name AS fuel_type " +
                "FROM car_specifications s " +
                "JOIN body_types bt ON bt.id = s.body_type_id " +
                "JOIN transmissions t ON t.id = s.transmission_id " +
                "JOIN fuel_types f ON f.id = s.fuel_type_id " +
                "ORDER BY s.car_id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                CarSpecification specification = new CarSpecification();
                specification.setCarId(resultSet.getLong("car_id"));
                specification.setVin(resultSet.getString("vin"));
                specification.setBodyType(resultSet.getString("body_type"));
                specification.setTransmission(resultSet.getString("transmission"));
                specification.setFuelType(resultSet.getString("fuel_type"));
                specification.setEngineVolume(resultSet.getBigDecimal("engine_volume"));
                specification.setDescription(resultSet.getString("description"));
                specifications.add(specification);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении характеристик автомобилей", e);
        }

        return specifications;
    }

    private List<NamedCatalogItem> findNamedItems(String table, String title) {
        List<NamedCatalogItem> items = new ArrayList<>();
        String sql = "SELECT id, name FROM " + table + " ORDER BY id";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                items.add(new NamedCatalogItem(
                        resultSet.getLong("id"),
                        resultSet.getString("name")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении " + title, e);
        }

        return items;
    }
}
