package carmarket.backend;

import carmarket.backend.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.RollbackException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DatabaseSchemaTest extends PostgresTestSupport {
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private Environment environment;
    @Autowired private TransactionTemplate transactions;
    @Autowired private EntityManagerFactory factory;
    @PersistenceContext private EntityManager em;

    @Test
    void migrationsValidateAndRepeatedRunDoesNotChangeData() {
        long before = jdbc.queryForObject("SELECT count(*) FROM cars", Long.class);
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cars", Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT code FROM roles ORDER BY code", String.class))
                .containsExactly("ADMIN", "MANAGER", "VIEWER");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void supportTablesAndForeignKeysArePresent() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tables).contains("refresh_sessions", "login_attempts", "car_notes",
                "audit_events", "csv_import_sessions", "car_tags", "purchase_requests");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.table_constraints "
                + "WHERE table_schema = 'public' AND constraint_type = 'FOREIGN KEY'", Integer.class))
                .isGreaterThanOrEqualTo(15);
    }

    @Test
    @Transactional
    void carSpecificationsTagsAndTimestampsRoundTripThroughJpa() {
        Car car = createCar();
        car.getTags().add("проверен");
        CarSpecification specification = new CarSpecification();
        specification.setCar(car);
        specification.setBodyType(catalog(new BodyType()));
        specification.setTransmission(catalog(new Transmission()));
        specification.setFuelType(catalog(new FuelType()));
        specification.setEngineVolume(new BigDecimal("2.0"));
        car.setSpecification(specification);
        em.persist(car);
        em.flush();
        Long id = car.getId();
        UUID publicId = car.getPublicId();
        em.clear();
        Car reloaded = em.find(Car.class, id);
        assertThat(reloaded.getPublicId()).isEqualTo(publicId);
        assertThat(reloaded.getPrice()).isEqualByComparingTo("1500000.00");
        assertThat(reloaded.getSpecification().getEngineVolume()).isEqualByComparingTo("2.0");
        assertThat(reloaded.getTags()).containsExactly("проверен");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.isArchived()).isFalse();
        assertThat(reloaded.isCertified()).isFalse();
    }

    @Test
    @Transactional
    void priceConstraintIsEnforcedByPostgres() {
        Car car = createCar();
        em.persist(car);
        em.flush();
        assertThatThrownBy(() -> jdbc.update("UPDATE cars SET price = -1 WHERE id = ?", car.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void externalCodeIsUniqueAndPlaintextPasswordsAreRejected() {
        Car car = createCar();
        em.persist(car);
        em.flush();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO user_credentials(user_id, login, password_hash) VALUES (?, ?, ?)",
                car.getSeller().getId(), "test-" + UUID.randomUUID(), "plaintext-is-forbidden"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void externalCodeCannotBeDuplicated() {
        Car first = createCar();
        em.persist(first);
        em.flush();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO cars(external_code, seller_id, model_id, year, mileage, price) "
                        + "VALUES (?, ?, ?, 2020, 0, 1)",
                first.getExternalCode(), first.getSeller().getId(), first.getModel().getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void concurrentJpaUpdatesRejectStaleVersion() {
        Long id = transactions.execute(status -> {
            Car car = createCar();
            em.persist(car);
            em.flush();
            return car.getId();
        });
        try (EntityManager first = factory.createEntityManager(); EntityManager second = factory.createEntityManager()) {
            first.getTransaction().begin();
            second.getTransaction().begin();
            Car firstCopy = first.find(Car.class, id);
            Car secondCopy = second.find(Car.class, id);
            firstCopy.setMileage(100);
            first.getTransaction().commit();
            secondCopy.setMileage(200);
            assertThatThrownBy(() -> second.getTransaction().commit()).isInstanceOf(RollbackException.class)
                    .hasRootCauseInstanceOf(org.hibernate.StaleObjectStateException.class);
        }
        assertThat(jdbc.queryForObject("SELECT mileage FROM cars WHERE id = ?", Integer.class, id)).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT version FROM cars WHERE id = ?", Long.class, id)).isEqualTo(1);
    }

    private Car createCar() {
        Role role = em.createQuery("select r from Role r where r.code = :code", Role.class)
                .setParameter("code", RoleCode.VIEWER).getSingleResult();
        UserAccount seller = new UserAccount();
        seller.setFullName("Тестовый продавец");
        seller.setRole(role);
        em.persist(seller);
        CarModel model = new CarModel();
        model.setName("Модель " + UUID.randomUUID());
        model.setBrand(catalog(new Brand()));
        em.persist(model);
        Car car = new Car();
        car.setExternalCode("TEST-" + UUID.randomUUID());
        car.setSeller(seller);
        car.setModel(model);
        car.setYear(2020);
        car.setMileage(50000);
        car.setPrice(new BigDecimal("1500000.00"));
        return car;
    }

    private <T extends CatalogEntry> T catalog(T entry) {
        entry.setName(UUID.randomUUID().toString().substring(0, 20));
        em.persist(entry);
        return entry;
    }
}
