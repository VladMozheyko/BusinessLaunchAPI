package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на изменение isFreeAddon.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на изменение isFreeAddon")
public class UpdateFreeAddonRequest {

    @Schema(description = "Новое значение isFreeAddon", example = "true")
    private Boolean isFreeAddon;
}
