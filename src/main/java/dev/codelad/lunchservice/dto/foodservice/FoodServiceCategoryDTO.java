package dev.codelad.lunchservice.dto.foodservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для категории из Food Service (кассовый аппарат).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FoodServiceCategoryDTO {

    /**
     * Уникальный идентификатор категории в Food Service.
     */
    private String id;

    /**
     * Название категории.
     */
    private String name;
}
