package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.LunchBooking;
import dev.codelad.lunchservice.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LunchBookingRepository extends JpaRepository<LunchBooking, java.util.UUID> {

    /**
     * Подсчитать текущую занятость заведения в слот.
     * Суммирует totalGuests всех броней с status != CANCELLED.
     */
    @Query("SELECT COALESCE(SUM(b.totalGuests), 0) FROM LunchBooking b " +
           "WHERE b.venue.id = :venueId " +
           "AND b.time = :time " +
           "AND b.status <> 'CANCELLED'")
    Integer sumTotalGuestsByVenueAndTime(
            @Param("venueId") Long venueId,
            @Param("time") LocalTime time
    );

    /**
     * Подсчитать занятость заведения на конкретную дату и время.
     */
    @Query("SELECT COALESCE(SUM(b.totalGuests), 0) FROM LunchBooking b " +
           "WHERE b.venue.id = :venueId " +
           "AND b.time = :time " +
           "AND b.status IN :statuses " +
           "AND b.createdAt >= :startDate AND b.createdAt < :endDate")
    int sumTotalGuestsByVenueAndTimeAndDate(
            @Param("venueId") Long venueId,
            @Param("time") LocalTime time,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Найти бронирование по коду отмены.
     */
    Optional<LunchBooking> findByCancelCode(String cancelCode);

    /**
     * Найти все бронирования пользователя, отсортированные по дате создания (новые первые).
     */
    List<LunchBooking> findByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    /**
     * Проверить, есть ли активные заказы с данным блюдом.
     * Активные статусы: PENDING (на подтверждении), APPROVED (одобрен).
     */
    @Query("SELECT DISTINCT b FROM LunchBooking b JOIN b.items i " +
           "WHERE i.dish.id = :dishId " +
           "AND b.status IN ('PENDING', 'APPROVED')")
    boolean existsByDishIdAndActiveOrders(@Param("dishId") java.util.UUID dishId);
}
