package carmarket.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FxmlButtonsTest {
    private static final Pattern BUTTON = Pattern.compile("<Button\\b([^>]*)/?>");

    @Test
    void everyFxmlButton_shouldHaveOnAction() throws Exception {
        String[] files = {
                "main-view.fxml", "request-dialog.fxml", "car-dialog.fxml", "user-dialog.fxml",
                "request-details.fxml", "related-data.fxml", "statistics-view.fxml"
        };
        int buttons = 0;
        for (String file : files) {
            String fxml = new String(Objects.requireNonNull(getClass().getResourceAsStream("/fxml/" + file))
                    .readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = BUTTON.matcher(fxml);
            while (matcher.find()) {
                buttons++;
                assertThat(matcher.group(1)).as(file).contains("onAction=");
            }
        }
        assertThat(buttons).isGreaterThanOrEqualTo(20);
    }
}
