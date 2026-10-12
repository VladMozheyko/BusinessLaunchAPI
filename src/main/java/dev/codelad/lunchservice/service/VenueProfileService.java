package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.VenueProfileRequest;
import dev.codelad.lunchservice.dto.response.VenueProfileResponse;
import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.entity.VenueSchedule;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
import dev.codelad.lunchservice.repository.VenueScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления карточками заведений.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueProfileService {

    private final VenueProfileRepository venueProfileRepository;
    private final VenueScheduleRepository venueScheduleRepository;

    /**
     * Создать или обновить карточку заведения.
     */
    @Transactional
    public VenueProfileResponse createOrUpdate(UUID franchiseUserId, VenueProfileRequest request) {
        VenueProfile profile = updateExisting(franchiseUserId, request)
                .orElseGet(() -> createNew(franchiseUserId, request));

        List<VenueSchedule> schedules = venueScheduleRepository.findByVenueProfileIdOrderByDayOfWeekAsc(profile.getId());
        return VenueProfileResponse.from(profile, schedules);
    }

    /**
     * Обновить существующую карточку, если она есть.
     */
    private java.util.Optional<VenueProfile> updateExisting(
            UUID franchiseUserId, VenueProfileRequest request) {

        if (!venueProfileRepository.existsByFranchiseUserId(franchiseUserId)) {
            return java.util.Optional.empty();
        }

        List<VenueProfile> existing = venueProfileRepository.findByFranchiseUserId(franchiseUserId);
        VenueProfile profile = existing.isEmpty() ? null : existing.getFirst();
        if (profile == null) {
            return java.util.Optional.empty();
        }

        updateProfileFields(profile, request);
        replaceSchedules(profile.getId(), request.getSchedules());

        return java.util.Optional.of(profile);
    }

    /**
     * Обновить поля карточки.
     */
    private void updateProfileFields(
            VenueProfile profile, VenueProfileRequest request) {
        profile.setName(request.getName());
        profile.setDescription(request.getDescription());
        profile.setLogoUrl(request.getLogoUrl());
        profile.setVenueId(request.getVenueId());
    }

    /**
     * Заменить расписание: удалить старое, сохранить новое.
     */
    private void replaceSchedules(
            Long profileId, List<VenueProfileRequest.ScheduleItem> scheduleItems) {

        venueScheduleRepository.deleteAll(
                venueScheduleRepository.findByVenueProfileIdOrderByDayOfWeekAsc(profileId));

        if (scheduleItems != null && !scheduleItems.isEmpty()) {
            venueProfileRepository.findById(profileId).ifPresent(profile ->
                    saveSchedules(profile, scheduleItems));
        }
    }

    /**
     * Создать новую карточку заведения.
     */
    private VenueProfile createNew(UUID franchiseUserId, VenueProfileRequest request) {
        VenueProfile profile = VenueProfile.builder()
                .franchiseUserId(franchiseUserId)
                .venueId(request.getVenueId())
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .active(true)
                .build();

        profile = venueProfileRepository.save(profile);

        if (request.getSchedules() != null) {
            saveSchedules(profile, request.getSchedules());
        }

        return profile;
    }

    /**
     * Сохранить список расписаний для карточки.
     */
    private void saveSchedules(VenueProfile profile, List<VenueProfileRequest.ScheduleItem> scheduleItems) {
        for (VenueProfileRequest.ScheduleItem item : scheduleItems) {
            VenueSchedule schedule = VenueSchedule.builder()
                    .venueProfile(profile)
                    .dayOfWeek(VenueSchedule.DayOfWeek.valueOf(item.getDayOfWeek()))
                    .working(item.getWorking() != null ? item.getWorking() : null)
                    .startTime(item.getStartTime())
                    .endTime(item.getEndTime())
                    .build();
            venueScheduleRepository.save(schedule);
        }
    }

    /**
     * Получить карточку заведения по ID.
     */
    public VenueProfileResponse getById(Long id) {
        VenueProfile profile = venueProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Карточка заведения не найдена с ID: " + id));

        List<VenueSchedule> schedules = venueScheduleRepository.findByVenueProfileIdOrderByDayOfWeekAsc(id);
        return VenueProfileResponse.from(profile, schedules);
    }

    /**
     * Получить активную карточку заведения франчайзи.
     */
    public VenueProfileResponse getByFranchiseUserId(UUID franchiseUserId) {
        VenueProfile profile = venueProfileRepository.findByFranchiseUserIdAndActiveIsTrue(franchiseUserId)
                .orElseThrow(() -> new RuntimeException("Активная карточка заведения не найдена для пользователя: " + franchiseUserId));

        List<VenueSchedule> schedules = venueScheduleRepository.findByVenueProfileIdOrderByDayOfWeekAsc(profile.getId());
        return VenueProfileResponse.from(profile, schedules);
    }

    /**
     * Удалить карточку заведения.
     */
    @Transactional
    public void delete(Long id) {
        VenueProfile profile = venueProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Карточка заведения не найдена с ID: " + id));

        venueProfileRepository.delete(profile);
        log.info("[VENUE] Карточка заведения {} удалена", id);
    }
}
