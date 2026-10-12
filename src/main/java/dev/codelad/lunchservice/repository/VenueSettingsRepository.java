package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.VenueSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для настроек заведения.
 */
@Repository
public interface VenueSettingsRepository extends JpaRepository<VenueSettings, Long> {

    /**
     * Найти настройки по ID заведения.
     */
    Optional<VenueSettings> findByVenueId(Long venueId);
}
