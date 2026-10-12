package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.VenueSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueScheduleRepository extends JpaRepository<VenueSchedule, Long> {

    List<VenueSchedule> findByVenueProfileIdOrderByDayOfWeekAsc(Long venueProfileId);

    List<VenueSchedule> findByVenueProfileIdAndDayOfWeek(Long venueProfileId, VenueSchedule.DayOfWeek dayOfWeek);
}
