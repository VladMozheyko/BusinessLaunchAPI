package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.CreateDishRequest;
import dev.codelad.lunchservice.dto.request.UpdateAvailabilityRequest;
import dev.codelad.lunchservice.dto.request.UpdateDishRequest;
import dev.codelad.lunchservice.dto.request.UpdateFreeAddonRequest;
import dev.codelad.lunchservice.dto.response.DishResponse;
import dev.codelad.lunchservice.entity.Dish;
import dev.codelad.lunchservice.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для управления блюдами.
 */
@Tag(name = ApiTags.DISHES, description = ApiTags.DISHES_DESC)
@RestController
@RequestMapping("/api/v1/lunch")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @PostMapping("/dish")
    @Operation(
            summary = "Создание блюда",
            description = "Создаёт новое блюдо в указанной категории. Проверяет существование категории.",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "201", description = "Блюдо успешно создано")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Категория не найдена")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<DishResponse> createDish(
            @Parameter(description = "Данные для создания блюда", required = true)
            @Valid @RequestBody CreateDishRequest request) {
        Dish dish = dishService.createDish(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DishResponse.from(dish));
    }

    @GetMapping("/dish")
    @Operation(
            summary = "Получить все блюда категории",
            description = "Возвращает все блюда для указанной категории. Доступно для GL, FO и LA.",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Список блюд")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public List<DishResponse> findByCategoryId(
            @Parameter(description = "ID категории", required = true)
            @RequestParam Long categoryId) {
        return dishService.findByCategoryId(categoryId);
    }

    @GetMapping("/dish/my")
    @Operation(
            summary = "Получить все мои блюда для заведения",
            description = "Возвращает все блюда для указанного заведения Франчайзи. Доступно для GL, FO и LA.",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Список блюд")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public List<DishResponse> findMyDishesByVenueId(
            @Parameter(description = "ID заведения", required = true)
            @RequestParam Long venueId) {
        return dishService.findByVenueId(venueId);
    }

    @PutMapping("/dish/{id}")
    @Operation(
            summary = "Обновление блюда",
            description = "Обновляет price, weight или weightUnit блюда. Можно обновлять только указанные поля.",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Блюдо обновлено")
    @ApiResponse(responseCode = "404", description = "Блюдо не найдено")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<DishResponse> updateDish(
            @Parameter(description = "UUID блюда", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Новые значения price/weight/weightUnit", required = true)
            @Valid @RequestBody UpdateDishRequest request) {
        Dish dish = dishService.updateDish(id, request);
        return ResponseEntity.ok(DishResponse.from(dish));
    }

    @PatchMapping("/dish/{id}/availability")
    @Operation(
            summary = "Изменение доступности",
            description = "Включает или выключает блюдо из меню. Доступно для GL, FO, LA и LE.",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Доступность обновлена")
    @ApiResponse(responseCode = "404", description = "Блюдо не найдено")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<DishResponse> updateAvailability(
            @Parameter(description = "UUID блюда", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Новое значение isAvailable", required = true)
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        Dish dish = dishService.updateAvailability(id, request);
        return ResponseEntity.ok(DishResponse.from(dish));
    }

    @PatchMapping("/dish/{id}/free-addon")
    @Operation(
            summary = "Изменение isFreeAddon",
            description = "Устанавливает или снимает флаг «бесплатное дополнение» (хлеб, компот и т.д.).",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Флаг обновлён")
    @ApiResponse(responseCode = "404", description = "Блюдо не найдено")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<DishResponse> updateFreeAddon(
            @Parameter(description = "UUID блюда", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Новое значение isFreeAddon", required = true)
            @Valid @RequestBody UpdateFreeAddonRequest request) {
        Dish dish = dishService.updateFreeAddon(id, request);
        return ResponseEntity.ok(DishResponse.from(dish));
    }

    @DeleteMapping("/dish/{id}")
    @Operation(
            summary = "Удаление блюда",
            description = "Полное удаление блюда из системы.",
            tags = {ApiTags.DISHES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "204", description = "Блюдо удалено")
    @ApiResponse(responseCode = "404", description = "Блюдо не найдено")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<Void> deleteDish(
            @Parameter(description = "UUID блюда", required = true)
            @PathVariable UUID id) {
        dishService.deleteDish(id);
        return ResponseEntity.noContent().build();
    }
}
