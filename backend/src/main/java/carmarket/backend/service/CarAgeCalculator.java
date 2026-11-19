package carmarket.backend.service;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Year;

@Component
public class CarAgeCalculator {
    private final Clock clock;
    public CarAgeCalculator(Clock clock) { this.clock = clock; }
    public int currentYear() { return Year.now(clock).getValue(); }
    public int ageYears(int productionYear) { return Math.max(0, currentYear() - productionYear); }
}
