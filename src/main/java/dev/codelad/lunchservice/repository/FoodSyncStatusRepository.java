package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.FoodSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodSyncStatusRepository extends JpaRepository<FoodSyncStatus, Long> {

    /**
     * Найти статус синхронизации для заведения.
     */
    Optional<FoodSyncStatus> findByVenueId(Long venueId);

    /**
     * Проверить, есть ли статус синхронизации для заведения.
     */
    boolean existsByVenueId(Long venueId);
}
