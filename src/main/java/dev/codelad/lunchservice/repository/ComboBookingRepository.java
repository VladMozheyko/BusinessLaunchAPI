package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.ComboBooking;
import dev.codelad.lunchservice.enums.ComboBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository для работы с бронированиями комбо.
 */
@Repository
public interface ComboBookingRepository extends JpaRepository<ComboBooking, UUID> {

    /**
     * Найти бронирование по коду отмены.
     */
    Optional<ComboBooking> findByCancelCode(String cancelCode);

    /**
     * Найти все бронирования по номеру телефона.
     */
    List<ComboBooking> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    /**
     * Найти все бронирования пользователя по external user ID.
     */
    List<ComboBooking> findByUserIdOrderByCreatedAtDesc(java.util.UUID userId);

    /**
     * Подсчитать сумму гостей в активных бронированиях на дату и время.
     */
    @Query("SELECT COALESCE(SUM(b.totalGuests), 0) FROM ComboBooking b " +
            "WHERE b.venueId = :venueId " +
            "AND b.status IN :statuses " +
            "AND b.time = :time " +
            "AND b.createdAt >= :startDate AND b.createdAt < :endDate")
    int sumTotalGuestsByVenueAndTimeAndDate(@Param("venueId") Long venueId,
                                            @Param("statuses") List<ComboBookingStatus> statuses,
                                            @Param("time") java.time.LocalTime time,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * Подсчитать текущее количество активных бронирований для combo на сегодня.
     */
    @Query("SELECT COALESCE(SUM(b.totalGuests), 0) FROM ComboBooking b " +
            "JOIN b.items i " +
            "WHERE i.comboId = :comboId " +
            "AND b.status IN :statuses " +
            "AND b.createdAt >= :startDate AND b.createdAt < :endDate")
    int sumTotalGuestsByComboIdAndDate(@Param("comboId") Long comboId,
                                       @Param("statuses") List<ComboBookingStatus> statuses,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
