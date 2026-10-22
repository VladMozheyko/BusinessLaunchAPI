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
     * Найти все активные блюда для категории.
     */
    List<Dish> findByCategoryIdAndActiveTrue(Long categoryId);

    /**
     * Найти все активные блюда для заведения (через категории).
     */
    @Query("SELECT d FROM Dish d WHERE d.category.venueId = :venueId AND d.active = true")
    List<Dish> findByVenueIdActive(@Param("venueId") Long venueId);

    /**
     * Найти все активные блюда для категории (с фильтром по active).
     */
    @Query("SELECT d FROM Dish d WHERE d.category.id = :categoryId AND d.active = true")
    List<Dish> findByCategoryIdActive(@Param("categoryId") Long categoryId);
}
