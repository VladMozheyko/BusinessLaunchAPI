package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.entity.VenueProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Найти все категории для заведения (включая скрытые).
     */
    List<Category> findByVenueProfile(VenueProfile venueProfile);

    /**
     * Найти все категории для заведения с фильтрацией по hidden.
     */
    @Query("SELECT c FROM Category c WHERE c.venueProfile = :venueProfile AND (:includeHidden = true OR c.hidden = false)")
    List<Category> findByVenueProfileAndIncludeHidden(
            @Param("venueProfile") VenueProfile venueProfile,
            @Param("includeHidden") boolean includeHidden);

    /**
     * Найти все категории с блюдами (JOIN FETCH для предотвращения N+1).
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.dishes WHERE c.venueProfile = :venueProfile AND (:includeHidden = true OR c.hidden = false)")
    List<Category> findByVenueProfileWithDishesAndIncludeHidden(
            @Param("venueProfile") VenueProfile venueProfile,
            @Param("includeHidden") boolean includeHidden);
}
