package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на обновление блюда (price, weight, weightUnit).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на обновление параметров блюда")
public class UpdateDishRequest {

    @Positive(message = "Вес должен быть положительным числом")
    @Schema(description = "Новый вес", example = "350")
    private Integer weight;

    @Schema(description = "Новая единица измерения", example = "г")
    private String weightUnit;

    @Positive(message = "Цена должна быть положительным числом")
    @Schema(description = "Новая цена в копейках", example = "40000")
    private Integer price;

    @Schema(description = "Новое количество доступных блюд (необязательно). null — не ограничено, 0 — ни одного, >0 — ограниченное количество", example = "100")
    private Integer quantity;
}
