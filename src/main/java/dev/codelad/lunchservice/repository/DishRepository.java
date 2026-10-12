package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DishRepository extends JpaRepository<Dish, UUID> {

    /**
     * Найти все блюда для категории.
     */
    List<Dish> findByCategoryId(Long categoryId);

    /**
     * Найти все блюда для заведения (через категории).
     */
    @Query("SELECT d FROM Dish d WHERE d.category.venueId = :venueId")
    List<Dish> findByVenueId(@Param("venueId") Long venueId);
}
