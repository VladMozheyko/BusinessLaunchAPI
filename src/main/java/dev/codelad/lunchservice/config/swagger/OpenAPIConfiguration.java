package dev.codelad.lunchservice.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Конфигурация Swagger/OpenAPI документации.
 * Настраивает заголовок API, схему аутентификации bearer JWT и описания тегов.
 */
@Configuration
public class OpenAPIConfiguration {

    @Value("${api.server.url}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LunchService API")
                        .version("0.0.1-SNAPSHOT")
                        .description("REST API для управления бизнес-ланчами: категории, блюда, комбо, бронирование")
                        .contact(new Contact()
                                .name("Поддержка LunchService")
                                .email("support@codelad.dev")))
                .addServersItem(new Server().url(serverUrl))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            openApi.getServers().clear();
            openApi.getServers().add(new Server().url(serverUrl));

            // Добавляем описания к тегам
            Map<String, String> tagDescriptions = new LinkedHashMap<>();
            tagDescriptions.put(ApiTags.PRESENTATION, ApiTags.PRESENTATION_DESC);
            tagDescriptions.put(ApiTags.CATEGORIES, ApiTags.CATEGORIES_DESC);
            tagDescriptions.put(ApiTags.DISHES, ApiTags.DISHES_DESC);
            tagDescriptions.put(ApiTags.COMBOS, ApiTags.COMBOS_DESC);
            tagDescriptions.put(ApiTags.LUNCH_BOOKINGS, ApiTags.LUNCH_BOOKINGS_DESC);
            tagDescriptions.put(ApiTags.GLOBAL_ADMIN, ApiTags.GLOBAL_ADMIN_DESC);
            tagDescriptions.put(ApiTags.FRANCHISE_OWNER, ApiTags.FRANCHISE_OWNER_DESC);
            tagDescriptions.put(ApiTags.LOCATION_ADMIN, ApiTags.LOCATION_ADMIN_DESC);
            tagDescriptions.put(ApiTags.USER, ApiTags.USER_DESC);

            tagDescriptions.forEach((name, description) ->
                    openApi.getTags().add(new io.swagger.v3.oas.models.tags.Tag().name(name).description(description))
            );
        };
    }
}
