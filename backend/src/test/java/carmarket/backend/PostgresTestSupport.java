package carmarket.backend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Testcontainers по умолчанию; KR3_TEST_DB_URL допускает явно выделенную пустую тестовую БД. */
abstract class PostgresTestSupport {
    private static final String EXTERNAL_URL = System.getenv("KR3_TEST_DB_URL");
    private static final PostgreSQLContainer<?> POSTGRES = EXTERNAL_URL == null ? new PostgreSQLContainer<>("postgres:16-alpine") : null;

    static {
        if (POSTGRES != null) POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EXTERNAL_URL == null ? POSTGRES.getJdbcUrl() : EXTERNAL_URL);
        registry.add("spring.datasource.username", () -> EXTERNAL_URL == null ? POSTGRES.getUsername() : System.getenv("KR3_TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> EXTERNAL_URL == null ? POSTGRES.getPassword() : System.getenv("KR3_TEST_DB_PASSWORD"));
        registry.add("carmarket.auth.secret-base64", () -> TEST_JWT_SECRET);
    }

    private static final String TEST_JWT_SECRET = java.util.Base64.getEncoder()
            .encodeToString(new java.security.SecureRandom().generateSeed(32));
}
