package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Расписание работы заведения на конкретный день недели.
 */
@Entity
@Table(name = "venue_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Связь с карточкой заведения.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_profile_id", nullable = false)
    private VenueProfile venueProfile;

    /**
     * День недели: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /**
     * Время начала работы (например, 12:00).
     */
    @Column(name = "start_time")
    private java.time.LocalTime startTime;

    /**
     * Время окончания работы (например, 17:00).
     */
    @Column(name = "end_time")
    private java.time.LocalTime endTime;

    /**
     * Работает ли день (если false — день выходной, startTime и endTime игнорируются).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean working = true;

    /**
     * Дни недели недели.
     */
    public enum DayOfWeek {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }
}
