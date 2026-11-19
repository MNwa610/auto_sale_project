package carmarket.backend;

import carmarket.backend.service.CarAgeCalculator;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

class CarAgeCalculatorTest {
    @Test void ageUsesTheServerYearAndDoesNotBecomeNegative() {
        var before = new CarAgeCalculator(Clock.fixed(Instant.parse("2026-12-31T23:59:59Z"), ZoneOffset.UTC));
        var after = new CarAgeCalculator(Clock.fixed(Instant.parse("2027-01-01T00:00:00Z"), ZoneOffset.UTC));
        assertThat(before.ageYears(2020)).isEqualTo(6);
        assertThat(after.ageYears(2020)).isEqualTo(7);
        assertThat(before.ageYears(2027)).isZero();
    }
}
