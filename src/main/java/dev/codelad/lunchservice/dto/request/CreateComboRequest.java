package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Запрос на создание комбо-набора.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание нового комбо-набора")
public class CreateComboRequest {

    @NotNull(message = "venueId обязателен")
    @Schema(description = "ID заведения", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long venueId;

    @NotBlank(message = "Название комбо обязательно")
    @Schema(description = "Название комбо-набора", example = "Комбо Бизнес", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Цена обязательна")
    @Positive(message = "Цена должна быть положительным числом")
    @Schema(description = "Цена комбо в копейках", example = "45000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer price;

    @NotNull(message = "dailyLimit обязателен")
    @Schema(description = "Лимит ланчей в день (0 = без лимита)", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer dailyLimit;

    @NotEmpty(message = "Список категорий не может быть пустым")
    @Schema(
            description = "Список ID категорий, включённых в комбо (по одному блюду из каждой категории)",
            example = "[1, 2, 3]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Long> categoryIds;

    @Schema(
            description = "Список ID блюд, которые входят в комбо бесплатно (дополнения без доплаты)",
            example = "[\"uuid-1\", \"uuid-2\"]"
    )
    private List<java.util.UUID> freeAddonDishIds;

    @Schema(
            description = "Активно ли комбо (по умолчанию true). Неактивные комбо не отображаются в меню.",
            example = "true"
    )
    private Boolean isActive = true;
}
