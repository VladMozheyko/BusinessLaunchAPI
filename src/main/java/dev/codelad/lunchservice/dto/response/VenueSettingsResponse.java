package dev.codelad.lunchservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ с настройками заведения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSettingsResponse {

    /**
     * ID записи настроек.
     */
    private Long id;

    /**
     * ID заведения.
     */
    private Long venueId;

    /**
     * Период ожидания перед автоматической пометкой NO_SHOW (в минутах).
     */
    private Integer noShowGracePeriodMinutes;
}
