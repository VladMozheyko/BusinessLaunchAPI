package dev.codelad.lunchservice.dto.foodservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для блюда из Food Service (кассовый аппарат).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FoodServiceDishDTO {

    /**
     * Уникальный идентификатор блюда в Food Service.
     */
    private String id;

    /**
     * Идентификатор категории в Food Service.
     */
    private String categoryId;

    /**
     * Название блюда.
     */
    private String name;

    /**
     * Вес или объём порции (число).
     */
    private Integer weight;

    /**
     * Единица измерения: "г" или "мл".
     */
    private String weightUnit;

    /**
     * Цена в копейках.
     */
    private Integer price;

    /**
     * В наличии / нет.
     */
    private Boolean available;

    /**
     * Тип блюда: MAIN (основное) или ADDON (дополнение).
     */
    private String dishType;
}
