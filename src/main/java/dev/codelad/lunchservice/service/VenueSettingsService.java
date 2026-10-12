package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.UpdateVenueSettingsRequest;
import dev.codelad.lunchservice.dto.response.VenueSettingsResponse;
import dev.codelad.lunchservice.entity.VenueSettings;
import dev.codelad.lunchservice.repository.VenueSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Сервис для управления настройками заведения.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VenueSettingsService {

    private final VenueSettingsRepository venueSettingsRepository;

    /**
     * Получить настройки заведения.
     * Если настроек нет — создать с значениями по умолчанию.
     */
    @Transactional(readOnly = true)
    public VenueSettingsResponse getSettings(Long venueId) {
        VenueSettings settings = venueSettingsRepository.findByVenueId(venueId)
                .orElseGet(() -> createDefaultSettings(venueId));

        return toResponse(settings);
    }

    /**
     * Обновить настройки заведения.
     */
    @Transactional
    public VenueSettingsResponse updateSettings(Long venueId, UpdateVenueSettingsRequest request) {
        VenueSettings settings = venueSettingsRepository.findByVenueId(venueId)
                .orElseGet(() -> VenueSettings.defaultFor(venueId));

        if (request.getNoShowGracePeriodMinutes() != null) {
            int minutes = request.getNoShowGracePeriodMinutes();
            if (minutes < 5 || minutes > 120) {
                throw new IllegalArgumentException("Период ожидания должен быть от 5 до 120 минут");
            }
            settings.setNoShowGracePeriodMinutes(minutes);
            log.info("[VENUE_SETTINGS] noShowGracePeriodMinutes обновлён для venueId={}: {} мин",
                    venueId, minutes);
        }

        VenueSettings saved = venueSettingsRepository.save(settings);
        return toResponse(saved);
    }

    /**
     * Получить grace period для заведения.
     * Если настроек нет — вернуть значение по умолчанию (20 минут).
     */
    @Transactional(readOnly = true)
    public int getNoShowGracePeriodMinutes(Long venueId) {
        return venueSettingsRepository.findByVenueId(venueId)
                .map(VenueSettings::getNoShowGracePeriodMinutes)
                .orElse(20); // default
    }

    private VenueSettings createDefaultSettings(Long venueId) {
        VenueSettings settings = VenueSettings.defaultFor(venueId);
        venueSettingsRepository.save(settings);
        log.info("[VENUE_SETTINGS] Созданы настройки по умолчанию для venueId={}", venueId);
        return settings;
    }

    private VenueSettingsResponse toResponse(VenueSettings settings) {
        return VenueSettingsResponse.builder()
                .id(settings.getId())
                .venueId(settings.getVenueId())
                .noShowGracePeriodMinutes(settings.getNoShowGracePeriodMinutes())
                .build();
    }
}
