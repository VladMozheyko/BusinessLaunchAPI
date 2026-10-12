package dev.codelad.lunchservice.util;

import dev.codelad.lunchservice.constant.Messages;
import dev.codelad.lunchservice.entity.VenueSchedule;
import dev.codelad.lunchservice.exception.BookingConflictException;
import dev.codelad.lunchservice.repository.VenueScheduleRepository;

import java.time.LocalTime;
import java.util.List;

/**
 * Утилита для проверки расписания заведения.
 */
public final class VenueScheduleUtils {

    private VenueScheduleUtils() {
        // Утилита, экземпляр не нужен
    }

    /**
     * Проверить, что заведение работает в выбранное время.
     *
     * @param venueScheduleRepository репозиторий расписания
     * @param venueId ID заведения
     * @param time время проверки
     * @throws BookingConflictException если заведение не работает в это время
     */
    public static void checkVenueSchedule(
            VenueScheduleRepository venueScheduleRepository,
            Long venueId,
            LocalTime time) {

        VenueSchedule.DayOfWeek dayOfWeek = DayOfWeekConverter.toVenueDayOfWeek(java.time.DayOfWeek.from(time));

        List<VenueSchedule> schedules = venueScheduleRepository.findByVenueProfileIdAndDayOfWeek(venueId, dayOfWeek);

        if (schedules.isEmpty()) {
            throw new BookingConflictException(
                    String.format(Messages.VENUE_NOT_WORKING_DAY, venueId));
        }

        VenueSchedule schedule = schedules.stream()
                .filter(s -> s.getWorking() != null && s.getWorking())
                .findFirst()
                .orElseThrow(() -> new BookingConflictException(
                        Messages.VENUE_NOT_WORKING));

        if (time.isBefore(schedule.getStartTime()) || time.isAfter(schedule.getEndTime())) {
            throw new BookingConflictException(
                    String.format(Messages.VENUE_OUTSIDE_HOURS,
                            schedule.getStartTime(), schedule.getEndTime(), time));
        }
    }
}
