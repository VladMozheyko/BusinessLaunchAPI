package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.Category;
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
    List<Category> findByVenueId(Long venueId);

    /**
     * Найти все категории для заведения с фильтрацией по hidden.
     */
    @Query("SELECT c FROM Category c WHERE c.venueId = :venueId AND (:includeHidden = true OR c.hidden = false)")
    List<Category> findByVenueIdAndIncludeHidden(
            @Param("venueId") Long venueId,
            @Param("includeHidden") boolean includeHidden);

    /**
     * Найти все категории с блюдами (JOIN FETCH для предотвращения N+1).
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.dishes WHERE c.venueId = :venueId AND (:includeHidden = true OR c.hidden = false)")
    List<Category> findByVenueIdWithDishesAndIncludeHidden(
            @Param("venueId") Long venueId,
            @Param("includeHidden") boolean includeHidden);
}
