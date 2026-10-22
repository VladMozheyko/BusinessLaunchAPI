package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на обновление комбо (название, цена, дневной лимит).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на обновление параметров комбо")
public class UpdateComboRequest {

    @Schema(description = "Новое название комбо", example = "Комбо Бизнес")
    private String name;

    @Positive(message = "Цена должна быть положительным числом")
    @Schema(description = "Новая цена в копейках", example = "50000")
    private Integer price;

    @Schema(description = "Новый дневной лимит (0 = без лимита)", example = "50")
    private Integer dailyLimit;
}
