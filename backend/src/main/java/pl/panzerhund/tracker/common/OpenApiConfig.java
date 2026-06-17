package pl.panzerhund.tracker.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata; the spec and Swagger UI are served by springdoc at /v3/api-docs and /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI announcementTrackerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Announcement Tracker API")
                .description("Tracking real-estate and car listings")
                .version("v1"));
    }
}
