package dev.codelad.lunchservice.dto.foodservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ответ от Food Service с категориями и блюдами.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FoodServiceMenuDTO {

    /**
     * Логотип заведения.
     */
    private String logoUrl;

    /**
     * Категории из Food Service.
     */
    @Builder.Default
    private List<FoodServiceCategoryDTO> categories = new ArrayList<>();

    /**
     * Блюда из Food Service.
     */
    @Builder.Default
    private List<FoodServiceDishDTO> dishes = new ArrayList<>();
}
