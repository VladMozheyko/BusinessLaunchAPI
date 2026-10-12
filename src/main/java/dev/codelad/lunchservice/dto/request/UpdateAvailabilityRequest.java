package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на изменение доступности блюда.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на изменение isAvailable")
public class UpdateAvailabilityRequest {

    @Schema(description = "Новое значение isAvailable", example = "false")
    private Boolean isAvailable;
}
