package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.CreateCategoryRequest;
import dev.codelad.lunchservice.dto.response.CategoryResponse;
import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.service.CategoryService;
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

    @PostMapping("/category")
    @Operation(
            summary = "Создание категории",
            description = "Создаёт новую категорию блюд. Проверяет существование заведения через venue-service.",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "201", description = "Категория успешно создана")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Заведение не найдено")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Parameter(description = "Данные для создания категории", required = true)
            @Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @GetMapping("/category/my")
    @Operation(
            summary = "Получить все мои категории для заведения",
            description = "Возвращает все категории для указанного заведения Франчайзи. Доступно для GL, FO и LA.",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Список категорий")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public List<CategoryResponse> findMyCategoriesByVenueId(
            @Parameter(description = "ID заведения", required = true)
            @RequestParam Long venueId) {
        return categoryService.findByVenueId(venueId);
    }

    @DeleteMapping("/category/{id}")
    @Operation(
            summary = "Удаление категории",
            description = "Полное удаление категории вместе со всеми связанными блюдами (cascade delete).",
            tags = {ApiTags.CATEGORIES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "204", description = "Категория удалена")
    @ApiResponse(responseCode = "404", description = "Категория не найдена")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID категории", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
