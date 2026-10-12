package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на создание блюда.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание нового блюда в категории")
public class CreateDishRequest {

    @NotNull(message = "categoryId обязателен")
    @Schema(description = "ID категории", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @NotBlank(message = "Название блюда обязательно")
    @Schema(description = "Название блюда", example = "Суп день", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Вес обязателен")
    @Positive(message = "Вес должен быть положительным числом")
    @Schema(description = "Вес или объём порции", example = "300", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer weight;

    @NotBlank(message = "Единица измерения обязательна")
    @Schema(description = "Единица измерения: «г» или «мл»", example = "г", requiredMode = Schema.RequiredMode.REQUIRED)
    private String weightUnit;

    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительным числом")
    @Schema(description = "Цена в копейках", example = "35000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer price;

    @Schema(description = "Блюдо доступно в меню", example = "true")
    private Boolean isAvailable = true;

    @Schema(description = "Бесплатное дополнение (входит в ланч без доплаты)", example = "false")
    private Boolean isFreeAddon = false;

    @Schema(description = "Количество доступных блюд (необязательно). null — не ограничено, 0 — ни одного, >0 — ограниченное количество", example = "100")
    private Integer quantity;
}
