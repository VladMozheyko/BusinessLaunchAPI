package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.VenueProfileRequest;
import dev.codelad.lunchservice.dto.response.VenueProfileResponse;
import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.entity.VenueSchedule;
import dev.codelad.lunchservice.repository.FoodServiceFeignClient;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
import dev.codelad.lunchservice.repository.VenueScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final FoodSyncService foodSyncService;
    private final FoodServiceFeignClient foodServiceFeignClient;

    /**
     * Получить или создать VenueProfile лениво.
     * Вызывается при первом запросе к данным заведения.
     * Создаёт пустой VenueProfile с venueId — пользователь позже заполнит name, description.
     * 
     * Если VenueProfile создан впервые — запускает async синхронизацию с Food Service.
     */
    @Transactional
    public VenueProfile getOrCreateVenueProfile(Long venueId) {
        return venueProfileRepository.findByVenueId(venueId)
                .orElseGet(() -> {
                    log.info("[VENUE] Ленивое создание VenueProfile: venueId={}", venueId);
                    VenueProfile newProfile = VenueProfile.builder()
                            .venueId(venueId)
                            .build();
                    VenueProfile saved = venueProfileRepository.save(newProfile);
                    
                    // Запускаем синхронизацию с Food Service (асинхронно)
                    triggerInitialSync(venueId);
                    
                    return saved;
                });
    }

    /**
     * Запустить начальную синхронизацию с Food Service.
     * Вызывается при создании нового VenueProfile.
     * Асинхронно — не блокирует ответ пользователю.
     */
    @Async("foodSyncExecutor")
    public void triggerInitialSync(Long venueId) {
        try {
            log.info("[INIT_SYNC] Запуск начальной синхронизации для venueId={}", venueId);
            foodSyncService.syncMenu(venueId, "INIT");
            log.info("[INIT_SYNC] Завершена для venueId={}", venueId);
        } catch (Exception e) {
            log.error("[INIT_SYNC] Ошибка синхронизации для venueId={}: {}", venueId, e.getMessage(), e);
        }
    }

    /**
     * Получить или обновить логотип заведения из Food Service.
     * Вызывается при запросе к VenueProfile (когда пользователь заполняет профиль).
     * 
     * @param venueId ID заведения
     * @return true если логотип успешно подтянут
     */
    @Transactional
    public boolean syncLogoFromFoodService(Long venueId) {
        return venueProfileRepository.findByVenueId(venueId)
                .map(profile -> {
                    try {
                        // Получаем данные из Food Service
                        var response = foodServiceFeignClient.getMenu(venueId);
                        
                        if (response.getBody() != null && response.getBody().getLogoUrl() != null) {
                            String logoUrl = response.getBody().getLogoUrl();
                            
                            // Используем общий метод из FoodSyncService
                            foodSyncService.updateLogo(venueId, logoUrl);
                            return true;
                        }
                    } catch (Exception e) {
                        log.warn("[VENUE_LOGO] Ошибка подтягивания логотипа для venueId={}: {}", venueId, e.getMessage());
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Создать или обновить карточку заведения.
     * Ищет по venueId (из Bar Auth JWT: bar_id/brew_id).
     */
    @Transactional
    public VenueProfileResponse createOrUpdate(Long venueId, VenueProfileRequest request) {
        VenueProfile profile = updateExisting(venueId, request)
                .orElseGet(() -> createNew(venueId, request));

        List<VenueSchedule> schedules = venueScheduleRepository.findByVenueProfileIdOrderByDayOfWeekAsc(profile.getId());
        return VenueProfileResponse.from(profile, schedules);
    }

    /**
     * Обновить существующую карточку, если она есть.
     */
    private java.util.Optional<VenueProfile> updateExisting(Long venueId, VenueProfileRequest request) {
        return venueProfileRepository.findByVenueId(venueId)
                .map(profile -> {
                    updateProfileFields(profile, request);
                    replaceSchedules(profile.getId(), request.getSchedules());
                    return profile;
                });
    }

    /**
     * Обновить поля карточки.
     */
    private void updateProfileFields(VenueProfile profile, VenueProfileRequest request) {
        profile.setName(request.getName());
        profile.setDescription(request.getDescription());
        profile.setLogoUrl(request.getLogoUrl());
    }

    /**
     * Заменить расписание: удалить старое, сохранить новое.
     */
    private void replaceSchedules(Long profileId, List<VenueProfileRequest.ScheduleItem> scheduleItems) {
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
    private VenueProfile createNew(Long venueId, VenueProfileRequest request) {
        VenueProfile profile = VenueProfile.builder()
                .venueId(venueId)
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .build();

        profile = venueProfileRepository.save(profile);

        if (request.getSchedules() != null) {
            saveSchedules(profile, request.getSchedules());
        }

        log.info("[VENUE] Создана новая карточка заведения: venueId={}, profileId={}", venueId, profile.getId());
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
     * Получить карточку заведения по venueId (из Bar Auth JWT: bar_id/brew_id).
     */
    public VenueProfileResponse getByVenueId(Long venueId) {
        VenueProfile profile = venueProfileRepository.findByVenueId(venueId)
                .orElseThrow(() -> new RuntimeException("Карточка заведения не найдена с venueId: " + venueId));

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
