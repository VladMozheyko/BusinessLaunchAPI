package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.VenueProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VenueProfileRepository extends JpaRepository<VenueProfile, Long> {

    /**
     * Найти карточку заведения по venueId (из Bar Auth JWT: bar_id/brew_id).
     */
    Optional<VenueProfile> findByVenueId(Long venueId);

    /**
     * Проверить, существует ли карточка заведения с указанным venueId.
     */
    boolean existsByVenueId(Long venueId);
}
