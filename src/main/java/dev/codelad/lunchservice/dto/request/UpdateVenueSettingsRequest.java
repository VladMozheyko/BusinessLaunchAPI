package dev.codelad.lunchservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на обновление настроек заведения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVenueSettingsRequest {

    /**
     * Период ожидания перед автоматической пометкой NO_SHOW (в минутах).
     */
    private Integer noShowGracePeriodMinutes;
}
