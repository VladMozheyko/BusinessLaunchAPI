package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.CreateCategoryRequest;
import dev.codelad.lunchservice.dto.response.CategoryFullResponse;
import dev.codelad.lunchservice.dto.response.CategoryResponse;
import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.security.PermissionService;
import dev.codelad.lunchservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для управления категориями блюд.
 */
@Tag(name = ApiTags.CATEGORIES, description = ApiTags.CATEGORIES_DESC)
@RestController
@RequestMapping("/api/v1/lunch")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final PermissionService permissionService;

    @PostMapping("/category")
    @Operation(
            summary = "Создание категории",
            description = "Создаёт новую категорию блюд. Проверяет существование заведения через venue-service.",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "201", description = "Категория успешно создана")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Заведение не найдено")
    public ResponseEntity<CategoryResponse> createCategory(
            @Parameter(description = "Данные для создания категории", required = true)
            @Valid @RequestBody CreateCategoryRequest request) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        Category category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Получить все категории с блюдами",
            description = "Возвращает все категории для заведения вместе со всеми блюдами. " +
                    "Параметры: includeHiddenCategories - включать скрытые категории, " +
                    "includeHiddenDishes - включать скрытые блюда.",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Список категорий с блюдами")
    public ResponseEntity<List<CategoryFullResponse>> getAllCategoriesWithDishes(
            @Parameter(description = "ID заведения", required = true)
            @RequestParam("venueId") Long venueId,
            @Parameter(description = "Включать скрытые категории (по умолчанию false)")
            @RequestParam(defaultValue = "false") boolean includeHiddenCategories,
            @Parameter(description = "Включать скрытые блюда (по умолчанию false)")
            @RequestParam(defaultValue = "false") boolean includeHiddenDishes) {
        
        List<CategoryFullResponse.CategoryWithDishes> categoriesWithDishes = 
                categoryService.findAllWithDishes(venueId, includeHiddenCategories, includeHiddenDishes);
        
        List<CategoryFullResponse> responses = categoriesWithDishes.stream()
                .map(CategoryFullResponse::from)
                .toList();
        
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/category/my")
    @Operation(
            summary = "Получить все мои категории для заведения",
            description = "Возвращает все категории для указанного заведения. " +
                    "includeHidden - включать скрытые категории (по умолчанию false).",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Список категорий")
    public List<CategoryResponse> findMyCategoriesByVenueId(
            @Parameter(description = "ID заведения", required = true)
            @RequestParam Long venueId,
            @Parameter(description = "Включать скрытые категории (по умолчанию false)")
            @RequestParam(defaultValue = "false") boolean includeHidden) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        return categoryService.findByVenueId(venueId, includeHidden);
    }

    @PatchMapping("/category/{id}/toggle")
    @Operation(
            summary = "Переключить видимость категории",
            description = "Если категория скрыта — покажет, если показана — скроет. " +
                    "Это позволяет создавать категории с тем же названием повторно.",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Категория обновлена")
    @ApiResponse(responseCode = "404", description = "Категория не найдена")
    public ResponseEntity<CategoryResponse> toggleCategory(
            @Parameter(description = "ID категории", required = true)
            @PathVariable Long id) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        Category category = categoryService.toggleCategory(id);
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    @DeleteMapping("/category/{id}/hard")
    @Operation(
            summary = "Физическое удаление категории",
            description = "Полное физическое удаление категории вместе со всеми связанными блюдами. Только для GLOBAL_ADMIN.",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "204", description = "Категория физически удалена")
    @ApiResponse(responseCode = "404", description = "Категория не найдена")
    @ApiResponse(responseCode = "409", description = "Нельзя удалить: есть скрытая категория")
    public ResponseEntity<Void> hardDeleteCategory(
            @Parameter(description = "ID категории", required = true)
            @PathVariable Long id) {
        permissionService.checkGlobalAdminAccess();
        categoryService.hardDeleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
