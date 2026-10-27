package dev.codelad.lunchservice.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Swagger/OpenAPI документации.
 * Настраивает заголовок API, схему аутентификации bearer JWT и порядок тегов.
 * 
 * <p>Порядок тегов в Swagger UI (строго заданный):
 * <ol>
 *   <li>Веню (карточки заведений)</li>
 *   <li>Синхронизация (Food Service)</li>
 *   <li>Вход/сервис (аутентификация)</li>
 *   <li>Настройки уведомлений</li>
 *   <li>Бронирование</li>
 *   <li>Комбо</li>
 *   <li>Блюда</li>
 *   <li>Категории</li>
 *   <li>Устройства</li>
 * </ol>
 */
@Slf4j
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

    /**
     * Настраивает теги Swagger в строгом порядке, определённом пользователем.
     * Теги добавляются в том порядке, в котором они отображаются в Swagger UI.
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            openApi.getServers().clear();
            openApi.getServers().add(new Server().url(serverUrl));

            // Удаляем автоматически добавленные теги (если есть)
            if (openApi.getTags() != null) {
                openApi.getTags().clear();
            }

            // Регистрируем теги в СТРОГО ЗАДАННОМ ПОРЯДКЕ
            registerTag(openApi, ApiTags.AUTH, ApiTags.AUTH_DESC);
            registerTag(openApi, ApiTags.VENUE_PROFILES, ApiTags.VENUE_PROFILES_DESC);
            registerTag(openApi, ApiTags.CATEGORIES, ApiTags.CATEGORIES_DESC);
            registerTag(openApi, ApiTags.DISHES, ApiTags.DISHES_DESC);
            registerTag(openApi, ApiTags.COMBOS, ApiTags.COMBOS_DESC);
            registerTag(openApi, ApiTags.LUNCH_BOOKINGS, ApiTags.LUNCH_BOOKINGS_DESC);
            registerTag(openApi, ApiTags.FOOD_SYNC, ApiTags.FOOD_SYNC_DESC);
            registerTag(openApi, ApiTags.PREFERENCES, ApiTags.PREFERENCES_DESC);
            registerTag(openApi, ApiTags.DEVICES, ApiTags.DEVICES_DESC);

            log.info("Зарегистрировано {} тегов в заданном порядке", openApi.getTags().size());
        };
    }

    /**
     * Вспомогательный метод для регистрации одного тега.
     */
    private void registerTag(OpenAPI openApi, String tagName, String tagDescription) {
        if (tagName != null && tagDescription != null) {
            Tag tag = new Tag().name(tagName).description(tagDescription);
            openApi.getTags().add(tag);
            log.debug("Зарегистрирован тег: {} = {}", tagName, tagDescription);
        }
    }
}
