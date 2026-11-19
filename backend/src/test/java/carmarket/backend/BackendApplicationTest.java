package carmarket.backend;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BackendApplicationTest.ErrorEndpoints.class)
class BackendApplicationTest extends PostgresTestSupport {

    @Autowired
    private MockMvc mvc;

    @Test
    void contextLoadsAndApiInfoIsAvailable() throws Exception {
        mvc.perform(get("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("carmarket-backend"))
                .andExpect(jsonPath("$.apiVersion").value("v1"));
    }

    @Test
    void healthIncludesDatabaseCheckWithoutExposingDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env").with(admin())).andExpect(status().isForbidden());
    }

    @Test
    void openApiDocumentsTheVersionedApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Автосалон — REST API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1'].get").exists())
                .andExpect(jsonPath("$.paths['/actuator/health']").doesNotExist());
    }

    @Test
    void swaggerEntryRedirectsToAvailableUi() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void missingResourceHasProblemDetailsWithoutQueryParameters() throws Exception {
        mvc.perform(get("/api/v1/missing").with(admin()).queryParam("token", "private-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/v1/missing"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("private-token"))));
    }

    @Test
    void unsupportedMethodPreservesStatusAndAllowHeader() throws Exception {
        mvc.perform(post("/api/v1").with(admin()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void invalidFieldsHaveStructuredErrors() throws Exception {
        mvc.perform(post("/api/v1/test/validation").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name[0]").value("Укажите название"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.instance").value("/api/v1/test/validation"));
    }

    @Test
    void unreadableAndUnknownFieldsDoNotExposeRequestContents() throws Exception {
        for (String body : new String[]{"{private-value", "{\"name\":\"Car\",\"private-value\":true}"}) {
            mvc.perform(post("/api/v1/test/validation").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Некорректный запрос."))
                    .andExpect(content().string(not(containsString("private-value"))));
        }
    }

    @Test
    void unexpectedErrorDoesNotExposeExceptionDetails() throws Exception {
        mvc.perform(get("/api/v1/test/failure").with(admin()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("internal-test-detail"))));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                new carmarket.backend.security.SessionAuthentication(new carmarket.backend.security.SessionUser(
                        1, java.util.UUID.randomUUID(), "error-test", "Тест", carmarket.backend.domain.RoleCode.ADMIN, false)));
    }

    // Эти маршруты существуют только в тестовом контексте, в рабочий JAR не попадают.
    @RestController
    static class ErrorEndpoints {
        @PostMapping("/api/v1/test/validation")
        public ValidationRequest validate(@Valid @RequestBody ValidationRequest request) {
            return request;
        }

        @GetMapping("/api/v1/test/failure")
        public void fail() {
            throw new IllegalStateException("internal-test-detail");
        }
    }

    record ValidationRequest(@NotBlank(message = "Укажите название") String name) {
    }
}
