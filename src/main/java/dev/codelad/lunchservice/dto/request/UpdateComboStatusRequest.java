package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на изменение активности комбо.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на изменение isActive")
public class UpdateComboStatusRequest {

    @Schema(description = "Новое значение isActive", example = "false")
    private Boolean isActive;
}
