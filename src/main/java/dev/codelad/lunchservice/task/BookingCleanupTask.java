package dev.codelad.lunchservice.task;

import dev.codelad.lunchservice.entity.ComboBooking;
import dev.codelad.lunchservice.entity.LunchBooking;
import dev.codelad.lunchservice.enums.BookingStatus;
import dev.codelad.lunchservice.enums.ComboBookingStatus;
import dev.codelad.lunchservice.repository.ComboBookingRepository;
import dev.codelad.lunchservice.repository.LunchBookingRepository;
import dev.codelad.lunchservice.service.VenueSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduled task для автоматической обработки просроченных бронирований.
 * Запускается каждую минуту.
 * <p>
 * Логика:
 * 1. Найти все APPROVED бронирования, у которых expiredAt < now()
 * 2. Проверить grace period: booking.time + gracePeriodMinutes < now()
 * 3. Если grace period истёк → перевести в NO_SHOW
 * 4. Increment noShowCount для пользователя
 * <p>
 * Grace period — настраиваемый период (по умолчанию 20 минут) после booking.time,
 * в течение которого менеджер может успеть перевести бронь в другой статус.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupTask {

    private final LunchBookingRepository lunchBookingRepository;
    private final ComboBookingRepository comboBookingRepository;
    private final VenueSettingsService venueSettingsService;

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
                // Проверяем grace period для заведения
                Long venueId = booking.getVenue().getVenueId();
                int gracePeriodMinutes = venueSettingsService.getNoShowGracePeriodMinutes(venueId);

                // booking.time + gracePeriod < now → только тогда помечаем NO_SHOW
                if (isGracePeriodExpired(booking, gracePeriodMinutes, now)) {
                    booking.setStatus(BookingStatus.NO_SHOW);
                    lunchBookingRepository.save(booking);
                    log.info("[BOOKING_NO_SHOW] Бронирование {} отмечено как NO_SHOW (venue={}, grace={} мин)",
                            booking.getId(), venueId, gracePeriodMinutes);
                } else {
                    log.debug("[BOOKING_CLEANUP] Пропущено бронирование {} — grace period не истёк ({} мин)",
                            booking.getId(), gracePeriodMinutes);
                }
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
                // Проверяем grace period для заведения
                int gracePeriodMinutes = venueSettingsService.getNoShowGracePeriodMinutes(booking.getVenueId());

                // booking.time + gracePeriod < now → только тогда помечаем NO_SHOW
                if (isComboGracePeriodExpired(booking, gracePeriodMinutes, now)) {
                    booking.setStatus(ComboBookingStatus.NO_SHOW);
                    comboBookingRepository.save(booking);
                    log.info("[BOOKING_NO_SHOW] Combo бронирование {} отмечено как NO_SHOW (venue={}, grace={} мин)",
                            booking.getId(), booking.getVenueId(), gracePeriodMinutes);
                } else {
                    log.debug("[BOOKING_CLEANUP] Пропущено combo бронирование {} — grace period не истёк ({} мин)",
                            booking.getId(), gracePeriodMinutes);
                }
            }
        } catch (Exception e) {
            log.error("[BOOKING_CLEANUP_ERROR] Ошибка при обработке просроченных combo booking: {}", e.getMessage(), e);
        }
    }

    /**
     * Проверить, истёк ли grace period для бронирования.
     * Grace period = booking.time + noShowGracePeriodMinutes.
     * Если grace period истёк → можно помечать NO_SHOW.
     */
    private boolean isGracePeriodExpired(LunchBooking booking, int gracePeriodMinutes, Instant now) {
        LocalTime bookingTime = booking.getTime();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime nowTime = now.atZone(zoneId).toLocalTime();
        LocalDate today = now.atZone(zoneId).toLocalDate();

        // Если booking.time ещё не наступила сегодня — не помечаем
        if (bookingTime.isAfter(nowTime)) {
            return false;
        }

        // Grace deadline = booking.time + gracePeriodMinutes
        LocalTime graceDeadline = bookingTime.plusMinutes(gracePeriodMinutes);

        // Если grace deadline тоже сегодня — сравниваем
        if (!graceDeadline.isBefore(today.atStartOfDay().toLocalTime())) {
            // Grace deadline сегодня (или позже)
            return nowTime.isAfter(graceDeadline);
        }

        // Grace deadline завтра (например, booking.time=23:50, grace=20min → 00:10)
        // Считаем что grace period истёк, если booking.time уже прошла
        return true;
    }

    /**
     * Проверить grace period для combo booking.
     */
    private boolean isComboGracePeriodExpired(ComboBooking booking, int gracePeriodMinutes, Instant now) {
        LocalTime bookingTime = booking.getTime();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime nowTime = now.atZone(zoneId).toLocalTime();
        LocalDate today = now.atZone(zoneId).toLocalDate();

        if (bookingTime.isAfter(nowTime)) {
            return false;
        }

        LocalTime graceDeadline = bookingTime.plusMinutes(gracePeriodMinutes);

        if (!graceDeadline.isBefore(today.atStartOfDay().toLocalTime())) {
            return nowTime.isAfter(graceDeadline);
        }

        return true;
    }

    /**
     * Ручное восстановление бронирования из NO_SHOW в APPROVED.
     * Вызывается менеджером, когда пользователь пришёл, но scheduler уже пометил как NO_SHOW.
     */
    @Transactional
    public LunchBooking restoreLunchBookingFromNoShow(java.util.UUID bookingId) {
        LunchBooking booking = lunchBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено: " + bookingId));

        if (booking.getStatus() != BookingStatus.NO_SHOW) {
            throw new IllegalStateException("Бронирование не в статусе NO_SHOW: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.APPROVED);
        lunchBookingRepository.save(booking);

        log.info("[BOOKING_RESTORE] Бронирование {} восстановлено из NO_SHOW в APPROVED", bookingId);
        return booking;
    }

    /**
     * Ручное восстановление combo бронирования из NO_SHOW в APPROVED.
     */
    @Transactional
    public ComboBooking restoreComboBookingFromNoShow(java.util.UUID bookingId) {
        ComboBooking booking = comboBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Combo бронирование не найдено: " + bookingId));

        if (booking.getStatus() != ComboBookingStatus.NO_SHOW) {
            throw new IllegalStateException("Combo бронирование не в статусе NO_SHOW: " + booking.getStatus());
        }

        booking.setStatus(ComboBookingStatus.APPROVED);
        comboBookingRepository.save(booking);

        log.info("[BOOKING_RESTORE] Combo бронирование {} восстановлено из NO_SHOW в APPROVED", bookingId);
        return booking;
    }
}
