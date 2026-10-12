package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Контроллер с информацией о ролях и правах доступа в системе.
 */
@Tag(name = ApiTags.PRESENTATION, description = ApiTags.PRESENTATION_DESC)
@RestController
@RequestMapping("/api/v1/lunch")
public class RoleInfoController {

    public static final String NAME = "name";
    public static final String SHORT = "short";
    public static final String DESCRIPTION = "description";
    public static final String CONTROLLER = "controller";
    public static final String ENDPOINT = "endpoint";
    public static final String ROLES = "roles";
    public static final String CATEGORY_CONTROLLER = "CategoryController";
    public static final String DISH_CONTROLLER = "DishController";

    @GetMapping("/roles")
    @Operation(
            summary = "Информация о ролях и правах доступа",
            description = "Возвращает полную информацию о ролях в системе LunchService, их сокращениях и правах доступа к API-эндпоинтам."
    )
    @ApiResponse(responseCode = "200", description = "Информация о ролях")
    public ResponseEntity<Map<String, Object>> getRoleInfo() {
        return ResponseEntity.ok(Map.of(
                ROLES, List.of(
                        Map.of(
                                NAME, "GLOBAL_ADMIN",
                                SHORT, "GL",
                                DESCRIPTION, "Глобальный администратор — полный доступ ко всем ресурсам"
                        ),
                        Map.of(
                                NAME, "FRANCHISE_OWNER",
                                SHORT, "FO",
                                DESCRIPTION, "Владелец франшизы — управление своими локациями"
                        ),
                        Map.of(
                                NAME, "LOCATION_ADMIN",
                                SHORT, "LA",
                                DESCRIPTION, "Локальный администратор — управление конкретной локацией"
                        ),
                        Map.of(
                                NAME, "USER",
                                SHORT, "U",
                                DESCRIPTION, "Пользователь — авторизованный пользователь"
                        )
                ),
                "permissions", List.of(
                        Map.of(
                                CONTROLLER, CATEGORY_CONTROLLER,
                                ENDPOINT, "POST /api/v1/lunch/category",
                                DESCRIPTION, "Создание категории",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, CATEGORY_CONTROLLER,
                                ENDPOINT, "PATCH /api/v1/lunch/category/{id}/addon",
                                DESCRIPTION, "Изменение isAddon",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, CATEGORY_CONTROLLER,
                                ENDPOINT, "DELETE /api/v1/lunch/category/{id}",
                                DESCRIPTION, "Удаление категории",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "POST /api/v1/lunch/dish",
                                DESCRIPTION, "Создание блюда",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "PUT /api/v1/lunch/dish/{id}",
                                DESCRIPTION, "Обновление блюда",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "PATCH /api/v1/lunch/dish/{id}/availability",
                                DESCRIPTION, "Изменение isAvailable",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "PATCH /api/v1/lunch/dish/{id}/free-addon",
                                DESCRIPTION, "Изменение isFreeAddon",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "DELETE /api/v1/lunch/dish/{id}",
                                DESCRIPTION, "Удаление блюда",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "POST /api/v1/lunch/combo",
                                DESCRIPTION, "Создание комбо",
                                ROLES, List.of("GL", "FO")
                        ),
                        Map.of(
                                CONTROLLER, DISH_CONTROLLER,
                                ENDPOINT, "PATCH /api/v1/lunch/combo/{id}/status",
                                DESCRIPTION, "Изменение isActive",
                                ROLES, List.of("GL", "FO", "LA")
                        ),
                        Map.of(
                                CONTROLLER, "LunchBookingController",
                                ENDPOINT, "POST /api/v1/lunch/booking",
                                DESCRIPTION, "Создание бронирования ланча",
                                ROLES, List.of("U")
                        )
                )
        ));
    }
}
