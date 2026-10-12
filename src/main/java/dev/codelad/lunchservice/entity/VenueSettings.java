package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Настройки заведения.
 * Хранит конфигурацию, специфичную для каждого заведения.
 */
@Entity
@Table(name = "venue_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_venue_settings_venue_id", columnNames = "venue_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID заведения.
     */
    @Column(name = "venue_id", nullable = false, unique = true)
    private Long venueId;

    /**
     * Период ожидания перед автоматической пометкой NO_SHOW (в минутах).
     * Отсчитывается от booking.time.
     * По умолчанию: 20 минут.
     */
    @Column(name = "no_show_grace_period_minutes", nullable = false)
    @Builder.Default
    private Integer noShowGracePeriodMinutes = 20;

    /**
     * Создать настройки по умолчанию для заведения.
     */
    public static VenueSettings defaultFor(Long venueId) {
        return VenueSettings.builder()
                .venueId(venueId)
                .noShowGracePeriodMinutes(20)
                .build();
    }
}
