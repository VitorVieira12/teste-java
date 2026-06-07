package com.compass.digitalbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI digitalBankOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Bank API")
                        .description("REST API for account management and fund transfers with JWT security")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Compass UOL Technical Test")
                                .email("candidate@example.com")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from POST /api/v1/auth/login")));
    }
}
