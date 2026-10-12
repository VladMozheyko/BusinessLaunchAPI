package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.entity.VenueSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Ответ с информацией о карточке заведения.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VenueProfileResponse {

    private Long id;
    private UUID franchiseUserId;
    private String name;
    private String description;
    private String logoUrl;
    private Boolean active;
    private List<ScheduleResponse> schedules;

    public static VenueProfileResponse from(VenueProfile profile, List<VenueSchedule> schedules) {
        return VenueProfileResponse.builder()
                .id(profile.getId())
                .franchiseUserId(profile.getFranchiseUserId())
                .name(profile.getName())
                .description(profile.getDescription())
                .logoUrl(profile.getLogoUrl())
                .active(profile.getActive())
                .schedules(schedules.stream()
                        .map(ScheduleResponse::from)
                        .toList())
                .build();
    }

    /**
     * Ответ с расписанием на один день.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleResponse {

        private Long id;
        private String dayOfWeek;
        private Boolean working;
        private LocalTime startTime;
        private LocalTime endTime;

        public static ScheduleResponse from(VenueSchedule schedule) {
            return ScheduleResponse.builder()
                    .id(schedule.getId())
                    .dayOfWeek(schedule.getDayOfWeek().name())
                    .working(schedule.getWorking())
                    .startTime(schedule.getStartTime())
                    .endTime(schedule.getEndTime())
                    .build();
        }
    }
}
