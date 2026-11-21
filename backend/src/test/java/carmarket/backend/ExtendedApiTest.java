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
@Import(ExtendedApiTest.ClockConfig.class)
class ExtendedApiTest extends PostgresTestSupport {
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


    @Test void analyticsAndAuditMatchControlledChanges() throws Exception {
        JsonNode before = tree(mvc.perform(get("/api/v1/analytics").with(as(viewer, RoleCode.VIEWER)))
                .andExpect(status().isOk()).andReturn());
        var first = car();
        first.put("price", 125);
        String id = create(first).get("id").asText();
        var second = car();
        second.put("status", "SOLD");
        second.put("price", 375);
        create(second);
        action(id, "archive", 0, 200);
        action(id, "restore", 1, 200);

        JsonNode after = tree(mvc.perform(get("/api/v1/analytics").with(as(viewer, RoleCode.VIEWER)))
                .andExpect(status().isOk()).andReturn());
        JsonNode totals = after.get("totals");
        JsonNode initial = before.get("totals");
        assertThat(totals.get("count").asLong()).isEqualTo(initial.get("count").asLong() + 2);
        assertThat(totals.get("available").asLong()).isEqualTo(initial.get("available").asLong() + 1);
        assertThat(totals.get("sold").asLong()).isEqualTo(initial.get("sold").asLong() + 1);
        assertThat(totals.get("certified").asLong()).isEqualTo(initial.get("certified").asLong() + 2);
        assertThat(totals.get("totalPrice").decimalValue())
                .isEqualByComparingTo(initial.get("totalPrice").decimalValue().add(BigDecimal.valueOf(500)));
        assertThat(totals.get("averagePrice").decimalValue()).isEqualByComparingTo(totals.get("totalPrice")
                .decimalValue().divide(BigDecimal.valueOf(totals.get("count").asLong()), 2, java.math.RoundingMode.HALF_UP));
        assertThat(after.get("brands").findValues("id").stream().anyMatch(node -> node.asLong() == brand)).isTrue();

        mvc.perform(get("/api/v1/audit").with(as(manager, RoleCode.MANAGER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit").with(as(admin, RoleCode.ADMIN)).param("entityType", "CAR")
                .param("entityId", id).param("size", "1").param("page", "1").param("sort", "createdAt,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].action").value("ARCHIVED"));
        mvc.perform(get("/api/v1/audit").with(as(admin, RoleCode.ADMIN)).param("entityId", id)
                .param("action", "RESTORED").param("from", "2020-01-01T00:00:00Z").param("to", "2040-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/audit").with(as(admin, RoleCode.ADMIN)).param("sort", "details,asc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/audit").with(as(admin, RoleCode.ADMIN)).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test void searchCombinesFiltersAndPaginatesStably() throws Exception {
        var one=car(); one.put("year",2031); one.put("price",100); one.put("externalCode","needle-%_"+UUID.randomUUID());
        JsonNode first=create(one); var two=car();two.put("price",100);create(two);
        var request=get("/api/v1/cars").param("sellerId",""+manager).param("priceMin","100").param("priceMax","100")
            .param("certified","true").param("tag","акция").param("sort","price,asc").param("size","1");
        JsonNode page=tree(mvc.perform(request.with(as(viewer,RoleCode.VIEWER))).andExpect(status().isOk()).andReturn());
        assertThat(page.get("totalElements").asInt()).isEqualTo(2);assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        JsonNode next=tree(mvc.perform(get("/api/v1/cars").param("sellerId",""+manager).param("sort","price,asc").param("size","1").param("page","1")
            .with(as(viewer,RoleCode.VIEWER))).andExpect(status().isOk()).andReturn());
        assertThat(next.get("content").get(0).get("id")).isNotEqualTo(page.get("content").get(0).get("id"));
        mvc.perform(get("/api/v1/cars").param("sellerId",""+manager).param("ageMax","0").with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/cars").param("q",one.get("externalCode").toString()).with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(first.get("id").asText()));
        for(String sort:List.of("price,asc","year,desc","mileage,asc"))
            mvc.perform(get("/api/v1/cars").param("sort",sort).with(as(viewer,RoleCode.VIEWER))).andExpect(status().isOk());
    }
    @Test void rejectsInvalidSearchParameters() throws Exception {
        for(var param:List.of(Map.entry("size","101"),Map.entry("page","-1"),Map.entry("sort","id,asc"),Map.entry("priceMin","-1"),Map.entry("yearMin","abc")))
            mvc.perform(get("/api/v1/cars").param(param.getKey(),param.getValue()).with(as(viewer,RoleCode.VIEWER))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/cars").param("priceMin","10").param("priceMax","1").with(as(viewer,RoleCode.VIEWER))).andExpect(status().isBadRequest());
    }
    @Test void archiveRestoreAndDeletePreserveStatusAndEnforceVersions() throws Exception {
        var request=car();request.put("status","RESERVED"); JsonNode c=create(request);String id=c.get("id").asText();
        mvc.perform(post("/api/v1/cars/"+id+"/archive").with(as(viewer,RoleCode.VIEWER)).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
            .andExpect(status().isForbidden());
        JsonNode archived=action(id,"archive",0,200);assertThat(archived.get("status").asText()).isEqualTo("RESERVED");
        action(id,"restore",0,409);JsonNode restored=action(id,"restore",1,200);assertThat(restored.get("status").asText()).isEqualTo("RESERVED");
        mvc.perform(delete("/api/v1/cars/"+id).param("version","2").with(as(admin,RoleCode.ADMIN))).andExpect(status().isConflict());
        action(id,"archive",2,200);
        mvc.perform(delete("/api/v1/cars/"+id).param("version","3").with(as(manager,RoleCode.MANAGER))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/cars/"+id).param("version","2").with(as(admin,RoleCode.ADMIN))).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/cars/"+id).param("version","3").with(as(admin,RoleCode.ADMIN))).andExpect(status().isNoContent());
        assertThat(audits(c,"DELETED")).isEqualTo(1);
    }
    @Test void bulkReportsIndependentResultsAndRejectsManagerDeletion() throws Exception {
        var a=create(car());var b=create(car());String id=a.get("id").asText();
        var body=Map.of("action","ARCHIVE","items",List.of(Map.of("id",id,"version",0),Map.of("id",b.get("id").asText(),"version",9),
            Map.of("id",UUID.randomUUID().toString(),"version",0),Map.of("id",id,"version",0)));
        JsonNode result=tree(mvc.perform(post("/api/v1/cars/bulk").with(as(manager,RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn());
        assertThat(result.findValues("status").stream().filter(JsonNode::isNumber).map(JsonNode::asInt).toList()).containsExactly(200,409,404,400);
        mvc.perform(post("/api/v1/cars/bulk").with(as(manager,RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("action","DELETE","items",List.of(Map.of("id",id,"version",1)))))).andExpect(status().isForbidden());
    }
    @Test void notesEnforceOwnershipCarBindingAndVersions() throws Exception {
        String id=create(car()).get("id").asText(), other=create(car()).get("id").asText(),url="/api/v1/cars/"+id+"/notes";
        JsonNode note=tree(mvc.perform(post(url).with(as(manager,RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Текст, с новой строкой\\nи кавычками \\\"да\\\"\",\"pinned\":false}" )).andExpect(status().isCreated()).andReturn());
        String nid=note.get("id").asText();assertThat(note.get("authorId").asLong()).isEqualTo(manager);
        assertThat(note.get("createdAt")).isEqualTo(note.get("updatedAt"));
        mvc.perform(put(url+"/"+nid).with(as(user("MANAGER"),RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"x\",\"pinned\":true,\"version\":0}")).andExpect(status().isForbidden());
        mvc.perform(put(url+"/"+nid).with(as(admin,RoleCode.ADMIN)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Обновлено\",\"pinned\":true,\"version\":0}")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(delete("/api/v1/cars/"+other+"/notes/"+nid).param("version","1").with(as(admin,RoleCode.ADMIN))).andExpect(status().isNotFound());
        mvc.perform(delete(url+"/"+nid).param("version","0").with(as(manager,RoleCode.MANAGER))).andExpect(status().isConflict());
        mvc.perform(delete(url+"/"+nid).param("version","1").with(as(manager,RoleCode.MANAGER))).andExpect(status().isNoContent());
    }
    @Test void csvPreviewDoesNotWriteApplyIsOwnedAndOneTime() throws Exception {
        String source=csv(car());JsonNode preview=preview(source,"NEW_ONLY");String id=preview.get("id").asText();
        assertThat(preview.get("validRows").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cars WHERE seller_id=?",Integer.class,manager)).isZero();
        applyCsv(id,admin,RoleCode.ADMIN,404);applyCsv(id,manager,RoleCode.MANAGER,200);applyCsv(id,manager,RoleCode.MANAGER,409);
        JsonNode duplicate=preview(source,"NEW_ONLY");assertThat(duplicate.get("invalidRows").asInt()).isEqualTo(1);
        applyCsv(duplicate.get("id").asText(),manager,RoleCode.MANAGER,409);
        var row=car(); row.put("externalCode",json.readTree(json.writeValueAsString(preview)).get("rows").get(0).get("data").get("externalCode").asText());
        row.put("vin","");row.put("price",123);
        JsonNode update=preview(csv(row),"UPDATE_BY_EXTERNAL_CODE");
        assertThat(update.get("validRows").asInt()).isEqualTo(1);
        applyCsv(update.get("id").asText(),manager,RoleCode.MANAGER,200);
        assertThat(jdbc.queryForObject("SELECT price FROM cars WHERE external_code=?",BigDecimal.class,row.get("externalCode"))).isEqualByComparingTo("123");
    }
    @Test void csvRowsErrorsDuplicatesExpiryAndStaleVersions() throws Exception {
        var row=car();String data=csv(row);String line=data.substring(data.indexOf('\n')+1);
        JsonNode duplicate=preview(data+line,"NEW_ONLY");assertThat(duplicate.get("invalidRows").asInt()).isEqualTo(1);
        row.put("price",-1);assertThat(preview(csv(row),"NEW_ONLY").get("invalidRows").asInt()).isEqualTo(1);
        row=car();JsonNode c=create(row);JsonNode check=preview(csv(row),"UPDATE_BY_EXTERNAL_CODE");
        action(c.get("id").asText(),"archive",0,200);applyCsv(check.get("id").asText(),manager,RoleCode.MANAGER,409);
        assertThat(jdbc.queryForObject("SELECT applied_at FROM csv_import_sessions WHERE id=?",java.sql.Timestamp.class,UUID.fromString(check.get("id").asText()))).isNull();
        JsonNode expired=preview(csv(car()),"NEW_ONLY");
        // Fixed test clock is 2030-06-01 12:00; set an explicit earlier valid interval.
        jdbc.update("UPDATE csv_import_sessions SET created_at='2029-01-01',expires_at='2029-01-02' WHERE id=?",UUID.fromString(expired.get("id").asText()));
        applyCsv(expired.get("id").asText(),manager,RoleCode.MANAGER,409);
    }
    @Test void exportUsesFiltersAndHandlesQuotedMultilineText() throws Exception {
        var row=car();row.put("description","Строка, \"кавычки\"\nперенос");create(row);
        var result=mvc.perform(get("/api/v1/cars/csv/export").param("sellerId",""+manager).with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andReturn();
        String source=result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode check=preview(source,"UPDATE_BY_EXTERNAL_CODE");assertThat(check.get("validRows").asInt()).isEqualTo(1);
        assertThat(check.get("rows").get(0).get("data").get("description").asText()).isEqualTo(row.get("description"));
    }

    @Test void csvApplyIsAtomicAndConcurrentReplayCommitsOnlyOnce() throws Exception {
        var first=car();var second=car();String csv=csv(first);String other=csv(second);
        String combined=csv+other.substring(other.indexOf('\n')+1);
        JsonNode check=preview(combined,"NEW_ONLY");String id=check.get("id").asText();
        create(second); // A conflicting code appeared after preview.
        applyCsv(id,manager,RoleCode.MANAGER,409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cars WHERE external_code=?",Integer.class,first.get("externalCode"))).isZero();
        var once=preview(csv(car()),"NEW_ONLY");String onceId=once.get("id").asText();
        List<Integer> statuses=concurrent(List.of(
            ()->mvc.perform(post("/api/v1/cars/csv/"+onceId+"/apply").with(as(manager,RoleCode.MANAGER))).andReturn().getResponse().getStatus(),
            ()->mvc.perform(post("/api/v1/cars/csv/"+onceId+"/apply").with(as(manager,RoleCode.MANAGER))).andReturn().getResponse().getStatus()));
        assertThat(statuses).containsExactlyInAnyOrder(200,409);
    }
    @Test void archivedFiltersNotesRolesAndCsvLimitsAreEnforced() throws Exception {
        var c=create(car());String id=c.get("id").asText();action(id,"archive",0,200);
        mvc.perform(get("/api/v1/cars").param("sellerId",""+manager).with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/cars").param("sellerId",""+manager).param("archived","true").with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(post("/api/v1/cars/"+id+"/notes").with(as(manager,RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"x\",\"pinned\":false}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/cars/csv/preview").param("mode","NEW_ONLY").with(as(viewer,RoleCode.VIEWER))
            .contentType("text/csv").content(csv(car()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cars/csv/preview").param("mode","NEW_ONLY").with(as(manager,RoleCode.MANAGER))
            .contentType("text/csv").content(new byte[2*1024*1024+1])).andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/v1/cars/csv/preview").param("mode","NEW_ONLY").with(as(manager,RoleCode.MANAGER))
            .contentType("text/csv").content(new byte[]{(byte)0xFF})).andExpect(status().isBadRequest());
    }

    @Test void relatedRequestsBlockDeletionAndNotesCascadeWithAuditPreserved() throws Exception {
        var c=create(car());String id=c.get("id").asText();
        JsonNode note=tree(mvc.perform(post("/api/v1/cars/"+id+"/notes").with(as(manager,RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\":\"Контроль\",\"pinned\":true}")).andExpect(status().isCreated()).andReturn());
        long requestId=jdbc.queryForObject("INSERT INTO purchase_requests(user_id,car_id) SELECT ?,id FROM cars WHERE public_id=? RETURNING id",
            Long.class,viewer,UUID.fromString(id));
        action(id,"archive",0,200);
        mvc.perform(delete("/api/v1/cars/"+id).param("version","1").with(as(admin,RoleCode.ADMIN))).andExpect(status().isConflict());
        assertThat(audits(c,"DELETED")).isZero();
        jdbc.update("DELETE FROM purchase_requests WHERE id=?",requestId);
        mvc.perform(delete("/api/v1/cars/"+id).param("version","1").with(as(admin,RoleCode.ADMIN))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM car_notes WHERE id=?",Integer.class,UUID.fromString(note.get("id").asText()))).isZero();
        assertThat(audits(c,"DELETED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE entity_type='CAR_NOTE' AND entity_id=?",Integer.class,note.get("id").asText())).isEqualTo(1);
    }
    @Test void exactFiltersAndInclusiveNumericBoundsUseAllCarFields() throws Exception {
        var c=create(car());
        var query=get("/api/v1/cars").param("brandId",""+brand).param("modelId",""+model).param("sellerId",""+manager)
            .param("bodyTypeId",""+bodyType).param("transmissionId",""+transmission).param("fuelTypeId",""+fuel)
            .param("status","AVAILABLE").param("yearMin","2020").param("yearMax","2020")
            .param("mileageMin","50000").param("mileageMax","50000").param("engineVolumeMin","2.0").param("engineVolumeMax","2.0")
            .param("ageMin","10").param("ageMax","10").param("size","100");
        mvc.perform(query.with(as(viewer,RoleCode.VIEWER))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(c.get("id").asText()));
        mvc.perform(get("/api/v1/cars").param("sellerId",""+manager).param("mileageMin","50001").with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/cars").param("sellerId",""+manager).param("engineVolumeMax","1.9").with(as(viewer,RoleCode.VIEWER)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    private JsonNode action(String id,String action,long version,int expected) throws Exception {
        return tree(mvc.perform(post("/api/v1/cars/"+id+"/"+action).with(as(manager,RoleCode.MANAGER)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("version",version)))).andExpect(status().is(expected)).andReturn());
    }
    private JsonNode preview(String source,String mode) throws Exception {
        return tree(mvc.perform(post("/api/v1/cars/csv/preview").param("mode",mode).with(as(manager,RoleCode.MANAGER))
            .contentType("text/csv;charset=UTF-8").content(source)).andExpect(status().isOk()).andReturn());
    }
    private void applyCsv(String id,long actor,RoleCode role,int expected) throws Exception {
        mvc.perform(post("/api/v1/cars/csv/"+id+"/apply").with(as(actor,role))).andExpect(status().is(expected));
    }
    private String csv(Map<String,Object> row) throws Exception {
        var out=new java.io.StringWriter();
        try(var printer=new org.apache.commons.csv.CSVPrinter(out,org.apache.commons.csv.CSVFormat.RFC4180.builder().setHeader(carmarket.backend.service.CarCsvCodec.HEADER).get())) {
            List<Object> values=new ArrayList<>();for(String h:carmarket.backend.service.CarCsvCodec.HEADER)values.add(h.equals("tags")?json.writeValueAsString(row.get(h)):row.get(h));
            printer.printRecord(values);
        }return out.toString();
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
