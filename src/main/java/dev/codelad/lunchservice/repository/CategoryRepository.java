package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Найти все категории для заведения.
     */
    List<Category> findByVenueId(Long venueId);
}
