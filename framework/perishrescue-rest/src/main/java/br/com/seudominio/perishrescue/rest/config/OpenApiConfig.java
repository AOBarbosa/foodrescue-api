package br.com.seudominio.perishrescue.rest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration: API metadata plus the JWT bearer security
 * scheme, so Swagger UI can authenticate against protected routes.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    /**
     * Builds the application's {@link OpenAPI} definition. Title and
     * description belong to the instance ({@code perishrescue.api.title},
     * {@code perishrescue.api.description}).
     *
     * @param title       the API title shown in Swagger UI
     * @param description the API description shown in Swagger UI
     * @return the OpenAPI definition
     */
    @Bean
    public OpenAPI customOpenAPI(
            @Value("${perishrescue.api.title:PerishRescue API}") String title,
            @Value("${perishrescue.api.description:REST API for rescuing perishable resources: offerers publish surplus that beneficiaries reserve and pick up.}")
            String description) {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version("0.1.0")
                        .description(description))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
