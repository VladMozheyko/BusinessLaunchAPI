package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.entity.Combo;
import lombok.Data;
import java.util.List;

/**
 * Полный ответ с информацией о комбо, включая категории, блюда и дополнения.
 */
@Data
public class ComboFullResponse {

    private Long id;
    private Long venueId;
    private String name;
    private Integer price;
    private boolean isActive;
    private Integer dailyLimit;
    private List<CategoryDishResponse> categories;
    private List<DishResponse> freeAddons;
    private List<DishResponse> addons;

    public static ComboFullResponse from(Combo combo) {
        ComboFullResponse response = new ComboFullResponse();
        response.id = combo.getId();
        response.venueId = combo.getVenueId();
        response.name = combo.getName();
        response.price = combo.getPrice();
        response.isActive = combo.isActive();
        response.dailyLimit = combo.getDailyLimit();
        response.categories = combo.getCategories().stream()
                .map(CategoryDishResponse::from)
                .toList();
        response.freeAddons = combo.getFreeAddons().stream()
                .map(DishResponse::from)
                .toList();
        response.addons = combo.getAddons().stream()
                .map(DishResponse::from)
                .toList();
        return response;
    }

    /**
     * Ответ с категорией и блюдами внутри.
     */
    @Data
    public static class CategoryDishResponse {
        private Long categoryId;
        private String categoryName;
        private List<DishResponse> dishes;

        public static CategoryDishResponse from(Category category) {
            CategoryDishResponse response = new CategoryDishResponse();
            response.categoryId = category.getId();
            response.categoryName = category.getName();
            response.dishes = category.getDishes().stream()
                    .map(DishResponse::from)
                    .toList();
            return response;
        }
    }
}
