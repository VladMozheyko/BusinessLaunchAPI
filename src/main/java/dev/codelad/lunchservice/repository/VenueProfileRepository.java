package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.VenueProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenueProfileRepository extends JpaRepository<VenueProfile, Long> {

    List<VenueProfile> findByFranchiseUserId(UUID franchiseUserId);

    Optional<VenueProfile> findByFranchiseUserIdAndActiveIsTrue(UUID franchiseUserId);

    boolean existsByFranchiseUserId(UUID franchiseUserId);
}
