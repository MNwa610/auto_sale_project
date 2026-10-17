package carmarket.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class StyleCssTest {
    @Test
    void styleCss_shouldContainMainUiStates() throws Exception {
        String css = new String(Objects.requireNonNull(getClass().getResourceAsStream("/css/style.css")).readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(css)
                .contains(".title-label")
                .contains(".table-view")
                .contains(".text-field")
                .contains(".button")
                .contains(".primary-button")
                .contains(".danger-button")
                .contains(".error-label")
                .contains(".warning-label")
                .contains(".hint-label");
    }
}
