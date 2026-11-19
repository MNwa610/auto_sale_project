package carmarket.backend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Одна одноразовая PostgreSQL на JVM тестов; Testcontainers удаляет её после завершения. */
abstract class PostgresTestSupport {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("carmarket.auth.secret-base64", () -> TEST_JWT_SECRET);
    }

    private static final String TEST_JWT_SECRET = java.util.Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(32));
}
