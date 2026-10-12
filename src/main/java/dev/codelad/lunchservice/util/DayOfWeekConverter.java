package dev.codelad.lunchservice.util;

import dev.codelad.lunchservice.entity.VenueSchedule;

import java.time.DayOfWeek;

/**
 * Утилита для конвертации дней недели между java.time и сущностями приложения.
 */
public final class DayOfWeekConverter {

    private DayOfWeekConverter() {
        // Утилита, экземпляр не нужен
    }

    /**
     * Конвертировать java.time.DayOfWeek в VenueSchedule.DayOfWeek.
     *
     * @param javaDayOfWeek Java enum день недели
     * @return VenueSchedule enum день недели
     */
    public static VenueSchedule.DayOfWeek toVenueDayOfWeek(DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> VenueSchedule.DayOfWeek.MONDAY;
            case TUESDAY -> VenueSchedule.DayOfWeek.TUESDAY;
            case WEDNESDAY -> VenueSchedule.DayOfWeek.WEDNESDAY;
            case THURSDAY -> VenueSchedule.DayOfWeek.THURSDAY;
            case FRIDAY -> VenueSchedule.DayOfWeek.FRIDAY;
            case SATURDAY -> VenueSchedule.DayOfWeek.SATURDAY;
            case SUNDAY -> VenueSchedule.DayOfWeek.SUNDAY;
        };
    }

    /**
     * Конвертировать VenueSchedule.DayOfWeek в java.time.DayOfWeek.
     *
     * @param venueDayOfWeek VenueSchedule enum день недели
     * @return Java enum день недели
     */
    public static DayOfWeek fromVenueDayOfWeek(VenueSchedule.DayOfWeek venueDayOfWeek) {
        return switch (venueDayOfWeek) {
            case MONDAY -> DayOfWeek.MONDAY;
            case TUESDAY -> DayOfWeek.TUESDAY;
            case WEDNESDAY -> DayOfWeek.WEDNESDAY;
            case THURSDAY -> DayOfWeek.THURSDAY;
            case FRIDAY -> DayOfWeek.FRIDAY;
            case SATURDAY -> DayOfWeek.SATURDAY;
            case SUNDAY -> DayOfWeek.SUNDAY;
        };
    }
}
