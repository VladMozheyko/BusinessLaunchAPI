package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.Dish;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ с категорией и всеми её блюдами (включая дополнения).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryFullResponse {

    private Long id;
    private String name;
    private Long venueId;
    private boolean hidden;
    private List<DishResponse> dishes;

    public static CategoryFullResponse from(CategoryWithDishes categoryWithDishes) {
        return CategoryFullResponse.builder()
                .id(categoryWithDishes.id())
                .name(categoryWithDishes.name())
                .venueId(categoryWithDishes.venueId())
                .hidden(categoryWithDishes.hidden())
                .dishes(DishResponse.fromList(categoryWithDishes.dishes()))
                .build();
    }

    /**
     * Временная структура для передачи категории с блюдами.
     */
    public record CategoryWithDishes(Long id, String name, Long venueId, boolean hidden, List<Dish> dishes) {}
}
