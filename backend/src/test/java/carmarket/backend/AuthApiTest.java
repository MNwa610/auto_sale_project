package carmarket.backend;

import carmarket.backend.service.BootstrapAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"carmarket.bootstrap.login=stage3-bootstrap",
        "carmarket.bootstrap.password=BootstrapTemp42!"})
@AutoConfigureMockMvc
@Import({AuthApiTest.TestClockConfig.class, AuthApiTest.ProbeEndpoints.class})
class AuthApiTest extends PostgresTestSupport {
    private static final String PASSWORD = "InitialPassword42!"; // Только одноразовые тестовые аккаунты.
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private MutableClock clock;
    @Autowired private BootstrapAdminService bootstrap;

    @BeforeEach void resetTime() { clock.now = Instant.parse("2030-01-01T12:00:00Z"); }

    @Test
    void loginIssuesJwtAndStoresOnlyRefreshHash() throws Exception {
        String login = user("VIEWER", false);
        JsonNode pair = login(login.toUpperCase(Locale.ROOT));
        assertThat(pair.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(pair.get("expiresInSeconds").asLong()).isEqualTo(600);
        assertThat(pair.get("refreshToken").asText()).hasSize(43);
        String hash = jdbc.queryForObject("SELECT token_hash FROM refresh_sessions WHERE user_id="
                + "(SELECT user_id FROM user_credentials WHERE login=?)", String.class, login);
        assertThat(hash).hasSize(64).isNotEqualTo(pair.get("refreshToken").asText());
        assertThat(hash).isEqualTo(carmarket.backend.security.TokenService.hash(pair.get("refreshToken").asText()));
        assertThat(jdbc.queryForObject("SELECT password_hash FROM user_credentials WHERE login=?", String.class, login))
                .startsWith("$2").isNotEqualTo(PASSWORD);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(pair)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action='LOGIN_SUCCEEDED' AND actor_login=?",
                Integer.class, login)).isEqualTo(1);
        String storedHash = jdbc.queryForObject("SELECT password_hash FROM user_credentials WHERE login=?", String.class, login);
        String events = jdbc.queryForObject("SELECT json_agg(e)::text FROM audit_events e WHERE actor_login=?", String.class, login);
        assertThat(events).doesNotContain(PASSWORD, storedHash, pair.get("accessToken").asText(), pair.get("refreshToken").asText(), hash);
    }

    @Test
    void badUnknownAndDisabledAccountsHaveSameFailure() throws Exception {
        String known = user("VIEWER", false);
        String disabled = user("VIEWER", false);
        jdbc.update("UPDATE users SET enabled=FALSE WHERE id=(SELECT user_id FROM user_credentials WHERE login=?)", disabled);
        for (String login : List.of(known, disabled, "unknown-" + UUID.randomUUID())) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("login", login, "password", "WrongPassword42!"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void lockoutPersistsAndExpiresWithoutSleep() throws Exception {
        String login = user("VIEWER", false);
        for (int i = 1; i <= 5; i++) {
            assertThat(attempt(login, "IncorrectPassword42!").getResponse().getStatus()).isEqualTo(i == 5 ? 429 : 401);
        }
        assertThat(attempt(login, PASSWORD).getResponse().getStatus()).isEqualTo(429);
        assertThat(jdbc.queryForObject("SELECT failed_attempts FROM login_attempts WHERE login=?", Integer.class, login)).isEqualTo(5);
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(attempt(login, PASSWORD).getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM login_attempts WHERE login=?", Integer.class, login)).isZero();
    }

    @Test
    void temporaryPasswordAllowsOnlyMeRefreshLogoutAndPasswordChange() throws Exception {
        String login = user("VIEWER", true);
        JsonNode old = login(login);
        mvc.perform(get("/api/v1/probe/view").header("Authorization", bearer(old)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(old)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
        JsonNode renewed = refresh(old, 200);
        JsonNode changed = body(mvc.perform(post("/api/v1/auth/password").header("Authorization", bearer(renewed))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD, "newPassword", "ChangedPassword43!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.mustChangePassword").value(false)).andReturn());
        mvc.perform(get("/api/v1/probe/view").header("Authorization", bearer(changed))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(renewed))).andExpect(status().isUnauthorized());
        refresh(renewed, 401);
        assertThat(attempt(login, PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(attempt(login, "ChangedPassword43!").getResponse().getStatus()).isEqualTo(200);
        String passwordAudit = jdbc.queryForObject("SELECT details::text FROM audit_events "
                + "WHERE action='PASSWORD_CHANGED' AND actor_login=?", String.class, login);
        assertThat(json.readTree(passwordAudit).get("revokedSessions").asInt()).isEqualTo(1);
        assertThat(json.readTree(passwordAudit).get("mustChangePassword").asBoolean()).isFalse();
        assertThat(passwordAudit).doesNotContain(PASSWORD, "ChangedPassword43!", old.get("refreshToken").asText());
    }

    @Test
    void rolesControlReadEditCatalogsAndPermanentDelete() throws Exception {
        mvc.perform(get("/api/v1/probe/view")).andExpect(status().isUnauthorized());
        for (String role : List.of("VIEWER", "MANAGER", "ADMIN")) {
            JsonNode pair = login(user(role, false));
            mvc.perform(get("/api/v1/probe/view").header("Authorization", bearer(pair))).andExpect(status().isOk());
            mvc.perform(post("/api/v1/probe/edit").header("Authorization", bearer(pair)))
                    .andExpect(status().is(role.equals("VIEWER") ? 403 : 204));
            mvc.perform(delete("/api/v1/cars/test-car").header("Authorization", bearer(pair)))
                    .andExpect(status().is(role.equals("ADMIN") ? 204 : 403));
            mvc.perform(post("/api/v1/catalogs/probe").header("Authorization", bearer(pair)))
                    .andExpect(status().is(role.equals("ADMIN") ? 204 : 403));
            mvc.perform(get("/api/v1/probe/method-admin").header("Authorization", bearer(pair)))
                    .andExpect(status().is(role.equals("ADMIN") ? 200 : 403));
        }
    }

    @Test
    void refreshRotatesAndReplayRevokesTheWholeFamily() throws Exception {
        JsonNode first = login(user("MANAGER", false));
        JsonNode second = refresh(first, 200);
        assertThat(second.get("refreshToken")).isNotEqualTo(first.get("refreshToken"));
        assertThat(second.get("refreshExpiresAt")).isEqualTo(first.get("refreshExpiresAt"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(first))).andExpect(status().isUnauthorized());
        refresh(first, 401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action='REFRESH_REUSE_DETECTED' "
                        + "AND actor_id IS NULL AND entity_id=(SELECT id::text FROM refresh_sessions WHERE token_hash=?)",
                Integer.class, carmarket.backend.security.TokenService.hash(first.get("refreshToken").asText()))).isEqualTo(1);
        refresh(second, 401);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(second))).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAccessCanBeRefreshedButExpiredSessionCannot() throws Exception {
        JsonNode first = login(user("VIEWER", false));
        clock.advance(Duration.ofMinutes(10));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(first))).andExpect(status().isUnauthorized());
        JsonNode second = refresh(first, 200);
        clock.advance(Duration.ofDays(7));
        refresh(second, 401);
    }

    @Test
    void logoutIsIdempotentAndDoesNotRevokeOtherDevices() throws Exception {
        String login = user("VIEWER", false);
        JsonNode first = login(login);
        JsonNode other = login(login);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("refreshToken", first.get("refreshToken").asText()))))
                    .andExpect(status().isNoContent());
        }
        refresh(first, 401);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(first))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(other))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action='LOGOUT' AND actor_login=?",
                Integer.class, login)).isEqualTo(1);
    }

    @Test
    void changingPasswordRevokesOtherDevicesAndRejectsWeakWrongOrReusedPasswords() throws Exception {
        String login = user("MANAGER", false);
        JsonNode first = login(login);
        JsonNode other = login(login);
        for (var invalid : List.of(Map.of("currentPassword", "WrongPassword42!", "newPassword", "ValidNewPassword43!"),
                Map.of("currentPassword", PASSWORD, "newPassword", "short"),
                Map.of("currentPassword", PASSWORD, "newPassword", PASSWORD))) {
            mvc.perform(post("/api/v1/auth/password").header("Authorization", bearer(first))
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalid)))
                    .andExpect(status().is(invalid.get("currentPassword").equals(PASSWORD) ? 400 : 401));
        }
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(first))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/password").header("Authorization", bearer(first)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", "ValidNewPassword43!"))))
                .andExpect(status().isOk());
        refresh(other, 401);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(other))).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedWrongPurposeMissingClaimsAndWrongIssuerTokensAreRejected() throws Exception {
        JsonNode pair = login(user("VIEWER", false));
        String access = pair.get("accessToken").asText();
        String[] parts = access.split("\\.");
        parts[1] = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"role\":\"ADMIN\"}".getBytes());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
        Map<String, Object> originalClaims = readClaims(access);
        for (String missing : List.of("sid", "aud", "exp")) {
            Map<String, Object> claims = new HashMap<>(originalClaims);
            claims.remove(missing);
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + sign(claims)))
                    .andExpect(status().isUnauthorized());
        }
        for (var invalid : List.of(Map.entry("iss", "other-issuer"), Map.entry("token_use", "refresh"))) {
            Map<String, Object> claims = new HashMap<>(originalClaims);
            claims.put(invalid.getKey(), invalid.getValue());
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + sign(claims)))
                    .andExpect(status().isUnauthorized());
        }
        // Даже подписанная роль ADMIN не заменяет фактические права VIEWER в БД.
        Map<String, Object> changedRole = new HashMap<>(originalClaims);
        changedRole.put("role", "ADMIN");
        mvc.perform(post("/api/v1/probe/edit").header("Authorization", "Bearer " + sign(changedRole)))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentAccountStateOverridesOldJwtClaims() throws Exception {
        String login = user("VIEWER", false);
        JsonNode pair = login(login);
        jdbc.update("UPDATE users SET role_id=(SELECT id FROM roles WHERE code='MANAGER') "
                + "WHERE id=(SELECT user_id FROM user_credentials WHERE login=?)", login);
        mvc.perform(post("/api/v1/probe/edit").header("Authorization", bearer(pair))).andExpect(status().isNoContent());
        jdbc.update("UPDATE users SET enabled=FALSE WHERE id=(SELECT user_id FROM user_credentials WHERE login=?)", login);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(pair))).andExpect(status().isUnauthorized());
        refresh(pair, 401);
    }

    @Test
    void simultaneousRefreshAllowsOneRotationAndReplayInvalidatesItsChild() throws Exception {
        JsonNode pair = login(user("VIEWER", false));
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) futures.add(pool.submit(() -> {
                start.await();
                return refreshResult(pair);
            }));
            start.countDown();
            List<MvcResult> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactlyInAnyOrder(200, 401);
            JsonNode child = body(results.stream().filter(result -> result.getResponse().getStatus() == 200).findFirst().orElseThrow());
            refresh(child, 401);
        }
    }

    @Test
    void concurrentFailuresAreNotLost() throws Exception {
        String login = "missing-" + UUID.randomUUID();
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 5; i++) tasks.add(() -> attempt(login, "IncorrectPassword42!").getResponse().getStatus());
            List<Integer> statuses = new ArrayList<>();
            for (var future : pool.invokeAll(tasks)) statuses.add(future.get());
            assertThat(statuses).containsExactlyInAnyOrder(401, 401, 401, 401, 429);
            assertThat(jdbc.queryForObject("SELECT failed_attempts FROM login_attempts WHERE login=?", Integer.class, login)).isEqualTo(5);
        }
    }

    @Test
    void bootstrapCreatesTemporaryAdminOnceWithoutResettingPassword() {
        jdbc.update("UPDATE users SET enabled=FALSE WHERE role_id=(SELECT id FROM roles WHERE code='ADMIN')");
        bootstrap.createInitialAdmin();
        String hash = jdbc.queryForObject("SELECT password_hash FROM user_credentials WHERE login='stage3-bootstrap'", String.class);
        assertThat(encoder.matches("BootstrapTemp42!", hash)).isTrue();
        bootstrap.createInitialAdmin();
        assertThat(jdbc.queryForObject("SELECT password_hash FROM user_credentials WHERE login='stage3-bootstrap'", String.class)).isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM users WHERE id="
                + "(SELECT user_id FROM user_credentials WHERE login='stage3-bootstrap')", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE action='BOOTSTRAP_ADMIN_CREATED' "
                        + "AND actor_id IS NULL AND entity_id=(SELECT user_id::text FROM user_credentials WHERE login='stage3-bootstrap')",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void invalidSecretFieldsReturnSafeErrors() throws Exception {
        String secret = "VerySensitiveValue".repeat(8);
        MvcResult login = attempt("unknown-login", secret);
        assertThat(login.getResponse().getStatus()).isEqualTo(400);
        assertThat(login.getResponse().getContentAsString()).doesNotContain(secret);
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", secret))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(secret))));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }

    private String user(String role, boolean temporary) {
        String login = "user-" + UUID.randomUUID();
        Long id = jdbc.queryForObject("INSERT INTO users(full_name, role_id, must_change_password) "
                + "SELECT 'Тестовый пользователь', id, ? FROM roles WHERE code=? RETURNING id", Long.class, temporary, role);
        jdbc.update("INSERT INTO user_credentials(user_id, login, password_hash) VALUES (?, ?, ?)", id, login, encoder.encode(PASSWORD));
        return login;
    }

    private MvcResult attempt(String login, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "password", password)))).andReturn();
    }
    private JsonNode login(String login) throws Exception {
        MvcResult result = attempt(login, PASSWORD);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        return body(result);
    }
    private MvcResult refreshResult(JsonNode pair) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", pair.get("refreshToken").asText())))).andReturn();
    }
    private JsonNode refresh(JsonNode pair, int status) throws Exception {
        MvcResult result = refreshResult(pair);
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        return body(result);
    }
    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    private String bearer(JsonNode pair) { return "Bearer " + pair.get("accessToken").asText(); }
    private Map<String, Object> readClaims(String access) throws Exception {
        Map<String, Object> claims = json.readValue(Base64.getUrlDecoder().decode(access.split("\\.")[1]),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        for (String field : List.of("iat", "exp")) if (claims.containsKey(field)) {
            claims.put(field, Instant.ofEpochSecond(((Number) claims.get(field)).longValue()));
        }
        return claims;
    }
    private String sign(Map<String, Object> claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().claims(values -> values.putAll(claims)).build())).getTokenValue();
    }

    static class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2030-01-01T12:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
    @TestConfiguration
    static class TestClockConfig {
        @Bean @Primary MutableClock testClock() { return new MutableClock(); }
    }
    @RestController
    static class ProbeEndpoints {
        @GetMapping("/api/v1/probe/view") public String view() { return "ok"; }
        @PostMapping("/api/v1/probe/edit") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
        public void edit() { }
        @DeleteMapping("/api/v1/cars/test-car") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
        public void deleteCar() { }
        @PostMapping("/api/v1/catalogs/probe") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
        public void catalog() { }
        @GetMapping("/api/v1/probe/method-admin") @PreAuthorize("hasRole('ADMIN')")
        public String admin() { return "ok"; }
    }
}
