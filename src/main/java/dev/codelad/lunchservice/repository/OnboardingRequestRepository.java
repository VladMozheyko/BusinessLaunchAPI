package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.OnboardingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OnboardingRequestRepository extends JpaRepository<OnboardingRequest, Long> {

    boolean existsByUserIdAndStatus(UUID userId, String status);

    List<OnboardingRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT o FROM OnboardingRequest o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OnboardingRequest> findByStatusOrderByCreatedAtAsc(@Param("status") String status);

    Optional<OnboardingRequest> findByIdAndStatus(Long id, String status);
}
