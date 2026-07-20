package com.dbtraining.tradeflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * OpenApiConfig — TICKET-I063
 * ============================================================================
 * WHAT:    Customises the OpenAPI document Springdoc generates.
 * HOW:     Single @Bean that returns an OpenAPI with project metadata.
 * WHY:     Swagger UI on /swagger-ui.html becomes the single source of truth
 *          for the API contract — the front-end team uses it instead of
 *          digging through controllers.
 * OBSERVE: After this is wired, the title in the top-left of Swagger UI is
 *          "TradeFlow API".
 * ============================================================================
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tradeflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TradeFlow API")
                        .description("Trade reconciliation REST API — Deutsche Bank TDI 2026 case study.")
                        .version("v1"));
    }
}
