package carmarket.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carMarketOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Автосалон — REST API")
                .version("v1")
                .description("API для веб-интерфейса КР3 и будущего Android-клиента КР4."));
    }
}
