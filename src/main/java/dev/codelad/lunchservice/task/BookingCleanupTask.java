package dev.codelad.lunchservice.task;

import dev.codelad.lunchservice.entity.ComboBooking;
import dev.codelad.lunchservice.entity.LunchBooking;
import dev.codelad.lunchservice.enums.BookingStatus;
import dev.codelad.lunchservice.enums.ComboBookingStatus;
import dev.codelad.lunchservice.repository.ComboBookingRepository;
import dev.codelad.lunchservice.repository.LunchBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled task для автоматической обработки просроченных бронирований.
 * Запускается каждую минуту.
 * <p>
 * Логика:
 * 1. Найти все APPROVED бронирования, у которых expiredAt < now()
 * 2. Перевести в NO_SHOW (пользователь не пришёл)
 * 3. Increment noShowCount для пользователя
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupTask {

    private final LunchBookingRepository lunchBookingRepository;
    private final ComboBookingRepository comboBookingRepository;

    /**
     * Запускать каждую минуту.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkExpiredBookings() {
        Instant now = Instant.now();
        log.debug("[BOOKING_CLEANUP] Запуск проверки просроченных бронирований");

        checkExpiredLunchBookings(now);
        checkExpiredComboBookings(now);
    }

    private void checkExpiredLunchBookings(Instant now) {
        try {
            List<LunchBooking> expiredBookings = lunchBookingRepository.findExpiredApprovedBookings(now);
            
            if (expiredBookings.isEmpty()) {
                return;
            }

            log.info("[BOOKING_CLEANUP] Найдено {} просроченных lunch booking(s)", expiredBookings.size());

            for (LunchBooking booking : expiredBookings) {
                booking.setStatus(BookingStatus.NO_SHOW);
                lunchBookingRepository.save(booking);
                log.info("[BOOKING_NO_SHOW] Бронирование {} отмечено как NO_SHOW (просрочено)", booking.getId());
            }
        } catch (Exception e) {
            log.error("[BOOKING_CLEANUP_ERROR] Ошибка при обработке просроченных lunch booking: {}", e.getMessage(), e);
        }
    }

    private void checkExpiredComboBookings(Instant now) {
        try {
            List<ComboBooking> expiredBookings = comboBookingRepository.findExpiredApprovedBookings(now);
            
            if (expiredBookings.isEmpty()) {
                return;
            }

            log.info("[BOOKING_CLEANUP] Найдено {} просроченных combo booking(s)", expiredBookings.size());

            for (ComboBooking booking : expiredBookings) {
                booking.setStatus(ComboBookingStatus.NO_SHOW);
                comboBookingRepository.save(booking);
                log.info("[BOOKING_NO_SHOW] Combo бронирование {} отмечено как NO_SHOW (просрочено)", booking.getId());
            }
        } catch (Exception e) {
            log.error("[BOOKING_CLEANUP_ERROR] Ошибка при обработке просроченных combo booking: {}", e.getMessage(), e);
        }
    }
}
