package carmarket.backend.config;

import carmarket.backend.service.BootstrapAdminService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapAdminConfig {
    @Bean
    @ConditionalOnProperty(name = "carmarket.bootstrap.enabled", havingValue = "true")
    public ApplicationRunner bootstrapAdmin(BootstrapAdminService service) {
        return args -> service.createInitialAdmin();
    }
}
