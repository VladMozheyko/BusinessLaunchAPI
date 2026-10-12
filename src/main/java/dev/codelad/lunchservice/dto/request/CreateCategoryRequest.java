package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на создание категории блюд.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание новой категории блюд")
public class CreateCategoryRequest {

    @NotNull(message = "venueId обязателен")
    @Schema(description = "ID заведения (из venue-service)", example = "1")
    private Long venueId;

    @NotBlank(message = "Название категории обязательно для заполнения.")
    @Size(max = 255, message = "Название слишком длинное")
    @Schema(description = "Название категории", example = "Супы", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
