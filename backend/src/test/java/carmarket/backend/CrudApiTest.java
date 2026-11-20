package carmarket.backend;

import carmarket.backend.domain.RoleCode;
import carmarket.backend.security.SessionAuthentication;
import carmarket.backend.security.SessionUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CrudApiTest.ClockConfig.class)
class CrudApiTest extends PostgresTestSupport {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private long admin, manager, viewer, brand, model, bodyType, transmission, fuel;

    @BeforeEach void fixtures() {
        admin = user("ADMIN"); manager = user("MANAGER"); viewer = user("VIEWER");
        brand = named("brands");
        model = jdbc.queryForObject("INSERT INTO car_models(brand_id,name) VALUES (?,?) RETURNING id", Long.class, brand, name());
        bodyType = named("body_types"); transmission = named("transmissions"); fuel = named("fuel_types");
    }

    @Test void createReadAndListExposeUuidReferencesAndAge() throws Exception {
        var request = car();
        JsonNode created = create(request);
        UUID.fromString(created.get("id").asText());
        assertThat(created.get("version").asLong()).isZero();
        assertThat(created.get("ageYears").asInt()).isEqualTo(10);
        assertThat(created.get("vin").asText()).isEqualTo(request.get("vin").toString().toUpperCase(Locale.ROOT));
        assertThat(created.get("tags")).hasSize(2);
        assertThat(created.get("brand").get("id").asLong()).isEqualTo(brand);
        assertThat(created.has("sellerId")).isFalse();
        assertThat(created.has("passwordHash")).isFalse();
        mvc.perform(get("/api/v1/cars/" + created.get("id").asText()).with(as(viewer, RoleCode.VIEWER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.externalCode").value(request.get("externalCode")));
        JsonNode list = tree(mvc.perform(get("/api/v1/cars").with(as(viewer, RoleCode.VIEWER))).andExpect(status().isOk()).andReturn());
        assertThat(list.findValuesAsText("id")).contains(created.get("id").asText());
        assertThat(audits(created, "CREATED")).isEqualTo(1);
    }

    @Test void updateRejectsStaleVersionAndIdenticalRequestIsANoop() throws Exception {
        var request = car(); JsonNode created = create(request);
        request.put("version", 0); request.put("price", new BigDecimal("1600000.00"));
        JsonNode updated = update(created, request, 200);
        assertThat(updated.get("version").asLong()).isEqualTo(1);
        update(created, request, 409);
        assertThat(audits(created, "UPDATED")).isEqualTo(1);
        request.put("version", 1);
        assertThat(update(created, request, 200).get("version").asLong()).isEqualTo(1);
        assertThat(audits(created, "UPDATED")).isEqualTo(1);
        request.remove("version"); update(created, request, 400);
    }

    @Test void specificationsAndTagsIncrementOwnerVersionAndAreAudited() throws Exception {
        var request = car(); JsonNode created = create(request);
        request.put("version", 0); request.put("description", "Новая запись характеристик");
        JsonNode first = update(created, request, 200);
        assertThat(first.get("version").asLong()).isEqualTo(1);
        request.put("version", 1); request.put("tags", List.of("новый"));
        JsonNode second = update(created, request, 200);
        assertThat(second.get("version").asLong()).isEqualTo(2);
        assertThat(second.get("tags").get(0).asText()).isEqualTo("новый");
        String details = jdbc.queryForObject("SELECT json_agg(details)::text FROM audit_events WHERE entity_id=? AND action='UPDATED'",
                String.class, created.get("id").asText());
        assertThat(details).contains("DESCRIPTION", "TAGS").doesNotContain("Новая запись характеристик");
    }

    @Test void invalidRequestsDoNotPersistCarsOrAudit() throws Exception {
        for (var replacement : List.of(Map.entry("price", (Object) 0), Map.entry("mileage", (Object) (-1)),
                Map.entry("year", (Object) 2032), Map.entry("vin", (Object) "short"),
                Map.entry("price", (Object) new BigDecimal("1.001")), Map.entry("tags", (Object) List.of("")),
                Map.entry("archived", (Object) true))) {
            var request = car(); request.put(replacement.getKey(), replacement.getValue());
            postCar(request, manager, RoleCode.MANAGER, 400);
        }
        var missing = car(); missing.remove("certified"); postCar(missing, manager, RoleCode.MANAGER, 400);
        var unknown = car(); unknown.put("modelId", Long.MAX_VALUE); postCar(unknown, manager, RoleCode.MANAGER, 404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cars WHERE seller_id=?", Integer.class, manager)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE actor_id=?", Integer.class, manager)).isZero();
    }

    @Test void duplicateCodeAndVinAreConflicts() throws Exception {
        var first = car(); create(first);
        var duplicateCode = car(); duplicateCode.put("externalCode", first.get("externalCode"));
        postCar(duplicateCode, manager, RoleCode.MANAGER, 409);
        var duplicateVin = car(); duplicateVin.put("vin", first.get("vin").toString().toUpperCase(Locale.ROOT));
        postCar(duplicateVin, manager, RoleCode.MANAGER, 409);
    }

    @Test void rolesAndDeferredDeletionAreEnforced() throws Exception {
        mvc.perform(get("/api/v1/cars")).andExpect(status().isUnauthorized());
        postCar(car(), viewer, RoleCode.VIEWER, 403);
        JsonNode created = create(car());
        mvc.perform(delete("/api/v1/cars/" + created.get("id").asText()).with(as(manager, RoleCode.MANAGER)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/cars/" + created.get("id").asText()).with(as(admin, RoleCode.ADMIN)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/catalogs/brands").with(as(manager, RoleCode.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(catalog(name(), null))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/catalogs/brands/" + brand).with(as(admin, RoleCode.ADMIN)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test void allCatalogKindsSupportCreateEditAndVersionedEnable() throws Exception {
        for (String kind : List.of("brands", "models", "body-types", "transmissions", "fuel-types")) {
            var request = catalog(name(), kind.equals("models") ? brand : null);
            JsonNode created = catalogPost(kind, request, 201);
            long id = created.get("id").asLong();
            request.put("version", 0); request.put("name", name());
            JsonNode updated = tree(mvc.perform(put("/api/v1/catalogs/" + kind + "/" + id).with(as(admin, RoleCode.ADMIN))
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request))).andExpect(status().isOk()).andReturn());
            assertThat(updated.get("version").asLong()).isEqualTo(1);
            patchEnabled(kind, id, false, 1, 200);
            patchEnabled(kind, id, true, 1, 409);
            JsonNode enabled = patchEnabled(kind, id, true, 2, 200);
            assertThat(enabled.get("version").asLong()).isEqualTo(3);
            mvc.perform(get("/api/v1/catalogs/" + kind + "/" + id).with(as(viewer, RoleCode.VIEWER)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }
    }

    @Test void catalogNamesAreUniqueAndModelsCannotChangeBrand() throws Exception {
        String unique = name(); catalogPost("brands", catalog(unique, null), 201);
        catalogPost("brands", catalog(" " + unique.toUpperCase(Locale.ROOT) + " ", null), 409);
        catalogPost("brands", catalog("x".repeat(51), null), 400);
        catalogPost("models", catalog(name(), null), 400);
        catalogPost("fuel-types", catalog(name(), brand), 400);
        var reparent = catalog(name(), named("brands")); reparent.put("version", 0);
        mvc.perform(put("/api/v1/catalogs/models/" + model).with(as(admin, RoleCode.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(reparent)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalogs/not-a-kind").with(as(viewer, RoleCode.VIEWER))).andExpect(status().isNotFound());
    }

    @Test void disabledCatalogKeepsExistingRelationsButCannotBeAssigned() throws Exception {
        var request = car(); JsonNode created = create(request);
        patchEnabled("body-types", bodyType, false, 0, 200);
        postCar(car(), manager, RoleCode.MANAGER, 409);
        request.put("version", 0); request.put("price", new BigDecimal("1700000.00"));
        JsonNode updated = update(created, request, 200);
        assertThat(updated.get("bodyType").get("enabled").asBoolean()).isFalse();
        patchEnabled("brands", brand, false, 0, 200);
        JsonNode parent = tree(mvc.perform(get("/api/v1/catalogs/models/" + model).with(as(viewer, RoleCode.VIEWER)))
                .andExpect(status().isOk()).andReturn());
        assertThat(parent.get("enabled").asBoolean()).isTrue();
        assertThat(parent.get("effectiveEnabled").asBoolean()).isFalse();
        // Проверяем именно запрет отключённой марки, вернув кузов в доступное состояние.
        patchEnabled("body-types", bodyType, true, 1, 200);
        postCar(car(), manager, RoleCode.MANAGER, 409);
        request.put("version", updated.get("version").asLong()); request.put("price", new BigDecimal("1800000.00"));
        update(created, request, 200);
    }

    @Test void disabledSellerCanRemainOnExistingCarAndSellerListDoesNotLeakCredentials() throws Exception {
        var request = car(); request.put("sellerId", viewer); JsonNode created = create(request);
        jdbc.update("UPDATE users SET enabled=FALSE WHERE id=?", viewer);
        var another = car(); another.put("sellerId", viewer); postCar(another, manager, RoleCode.MANAGER, 409);
        request.put("version", 0); request.put("mileage", 70000); update(created, request, 200);
        JsonNode sellers = tree(mvc.perform(get("/api/v1/cars/sellers").with(as(admin, RoleCode.ADMIN)))
                .andExpect(status().isOk()).andReturn());
        for (JsonNode seller : sellers) {
            assertThat(seller.size()).isEqualTo(2);
            assertThat(seller.get("id").asLong()).isNotEqualTo(viewer);
        }
    }

    @Test void nextModelYearHasZeroAgeAndArchivedCarCannotBeEdited() throws Exception {
        var request = car(); request.put("year", 2031); JsonNode created = create(request);
        assertThat(created.get("ageYears").asInt()).isZero();
        jdbc.update("UPDATE cars SET archived=TRUE WHERE public_id=?", UUID.fromString(created.get("id").asText()));
        request.put("version", 0); request.put("mileage", 70000);
        JsonNode conflict = update(created, request, 409);
        assertThat(conflict.get("code").asText()).isEqualTo("ARCHIVED_CAR");
        mvc.perform(get("/api/v1/cars/" + created.get("id").asText()).with(as(viewer, RoleCode.VIEWER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));
    }

    @Test void simultaneousUpdatesWithOneVersionAllowOnlyOneCommit() throws Exception {
        var first = car(); JsonNode created = create(first); first.put("version", 0); first.put("mileage", 100);
        var second = new LinkedHashMap<>(first); second.put("mileage", 200);
        List<Integer> statuses = concurrent(List.of(
                () -> updateResult(created, first).getResponse().getStatus(),
                () -> updateResult(created, second).getResponse().getStatus()));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(audits(created, "UPDATED")).isEqualTo(1);
    }

    @Test void concurrentCreatesCannotDuplicateExternalCode() throws Exception {
        var first = car(); var second = car(); second.put("externalCode", first.get("externalCode"));
        List<Integer> statuses = concurrent(List.of(
                () -> postCarResult(first, manager, RoleCode.MANAGER).getResponse().getStatus(),
                () -> postCarResult(second, manager, RoleCode.MANAGER).getResponse().getStatus()));
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cars WHERE external_code=?", Integer.class, first.get("externalCode"))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE actor_id=? AND action='CREATED'", Integer.class, manager)).isEqualTo(1);
    }

    private LinkedHashMap<String, Object> car() {
        var result = new LinkedHashMap<String, Object>();
        result.put("externalCode", "CAR-" + UUID.randomUUID()); result.put("sellerId", manager); result.put("modelId", model);
        result.put("year", 2020); result.put("mileage", 50000); result.put("price", new BigDecimal("1500000.00"));
        result.put("vin", "abc" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        result.put("bodyTypeId", bodyType); result.put("transmissionId", transmission); result.put("fuelTypeId", fuel);
        result.put("engineVolume", new BigDecimal("2.0")); result.put("description", "Описание");
        result.put("status", "AVAILABLE"); result.put("certified", true); result.put("tags", List.of(" АКЦИЯ ", "акция", "ПРОВЕРЕН"));
        return result;
    }
    private LinkedHashMap<String, Object> catalog(String name, Long brandId) {
        var result = new LinkedHashMap<String, Object>(); result.put("name", name); result.put("enabled", true);
        if (brandId != null) result.put("brandId", brandId); return result;
    }
    private JsonNode create(Map<String, Object> body) throws Exception { return postCar(body, manager, RoleCode.MANAGER, 201); }
    private JsonNode postCar(Map<String, Object> body, long actor, RoleCode role, int expected) throws Exception {
        MvcResult result = postCarResult(body, actor, role); assertThat(result.getResponse().getStatus()).isEqualTo(expected);
        if (expected == 201) assertThat(result.getResponse().getHeader("Location")).startsWith("/api/v1/cars/");
        return tree(result);
    }
    private MvcResult postCarResult(Map<String, Object> body, long actor, RoleCode role) throws Exception {
        return mvc.perform(post("/api/v1/cars").with(as(actor, role)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andReturn();
    }
    private MvcResult updateResult(JsonNode created, Map<String, Object> body) throws Exception {
        return mvc.perform(put("/api/v1/cars/" + created.get("id").asText()).with(as(manager, RoleCode.MANAGER))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andReturn();
    }
    private JsonNode update(JsonNode created, Map<String, Object> body, int expected) throws Exception {
        MvcResult result = updateResult(created, body); assertThat(result.getResponse().getStatus()).isEqualTo(expected); return tree(result);
    }
    private JsonNode catalogPost(String kind, Map<String, Object> body, int expected) throws Exception {
        return tree(mvc.perform(post("/api/v1/catalogs/" + kind).with(as(admin, RoleCode.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn());
    }
    private JsonNode patchEnabled(String kind, long id, boolean enabled, long version, int expected) throws Exception {
        return tree(mvc.perform(patch("/api/v1/catalogs/" + kind + "/" + id + "/enabled").with(as(admin, RoleCode.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("enabled", enabled, "version", version))))
                .andExpect(status().is(expected)).andReturn());
    }
    private int audits(JsonNode car, String action) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE entity_id=? AND action=?", Integer.class, car.get("id").asText(), action);
    }
    private long named(String table) {
        // Имена таблиц фиксированы в тестовых fixtures, не берутся из HTTP.
        return jdbc.queryForObject("INSERT INTO " + table + "(name) VALUES (?) RETURNING id", Long.class, name());
    }
    private long user(String role) {
        Long id = jdbc.queryForObject("INSERT INTO users(full_name,role_id,must_change_password) "
                + "SELECT 'Участник CRUD',id,FALSE FROM roles WHERE code=? RETURNING id", Long.class, role);
        jdbc.update("INSERT INTO user_credentials(user_id,login) VALUES (?,?)", id, "crud-" + UUID.randomUUID()); return id;
    }
    private RequestPostProcessor as(long id, RoleCode role) {
        return authentication(new SessionAuthentication(new SessionUser(id, UUID.randomUUID(), "crud", "Тест", role, false)));
    }
    private JsonNode tree(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    private static String name() { return "Test-" + UUID.randomUUID().toString().substring(0, 8); }
    private List<Integer> concurrent(List<Callable<Integer>> calls) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(calls.size())) {
            CountDownLatch start = new CountDownLatch(1); List<Future<Integer>> futures = new ArrayList<>();
            for (var call : calls) futures.add(pool.submit(() -> { start.await(); return call.call(); }));
            start.countDown(); List<Integer> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS)); return results;
        }
    }
    @TestConfiguration static class ClockConfig {
        @Bean @Primary Clock crudClock() { return Clock.fixed(Instant.parse("2030-06-01T12:00:00Z"), ZoneOffset.UTC); }
    }
}
