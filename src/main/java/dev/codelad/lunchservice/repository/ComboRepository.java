package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {

    /**
     * Найти все комбо для заведения.
     */
    List<Combo> findByVenueId(Long venueId);

    /**
     * Проверить, есть ли активные заказы для блюд в категориях комбо.
     */
    @Query("SELECT COUNT(DISTINCT i.id) > 0 FROM Combo c " +
            "JOIN c.categories cat " +
            "JOIN Dish d ON d.category.id = cat.id " +
            "JOIN LunchOrderItem i ON i.dish.id = d.id " +
            "JOIN LunchBooking b ON b.id = i.booking.id " +
            "WHERE c.id = :comboId " +
            "AND b.status IN ('PENDING', 'APPROVED')")
    boolean existsActiveOrders(@Param("comboId") Long comboId);
}
