package dev.codelad.lunchservice.task;

import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.repository.CategoryRepository;
import dev.codelad.lunchservice.service.FoodSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled task для автоматической синхронизации меню из Food Service.
 * Запускается раз в 24 часа в ночное время (03:00 AM).
 * 
 * Логика:
 * 1. Найти все заведения с синхронизированными категориями
 * 2. Для каждого заведения запустить синхронизацию
 * 3. Локальные данные пользователя (syncedFromFood=false) сохраняются
 * 4. Синхронизированные данные (syncedFromFood=true) обновляются
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodSyncTask {

    private final FoodSyncService foodSyncService;
    private final CategoryRepository categoryRepository;

    /**
     * Кэш запущенных синхронизаций для предотвращения параллельных запусков.
     */
    private final Set<Long> syncingVenues = ConcurrentHashMap.newKeySet();

    /**
     * Автоматическая синхронизация всех заведений раз в 24 часа.
     * Запускается в 03:00 AM по Москве.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Moscow")
    public void scheduledSyncAllVenues() {
        log.info("[FOOD_SYNC_SCHEDULED] Запуск автоматической синхронизации всех заведений");

        try {
            // Находим все venueId с синхронизированными категориями
            List<Long> venueIds = categoryRepository.findAll().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getSyncedFromFood()))
                    .map(c -> c.getVenueProfile().getVenueId())
                    .distinct()
                    .toList();

            if (venueIds.isEmpty()) {
                log.info("[FOOD_SYNC_SCHEDULED] Нет заведений для синхронизации");
                return;
            }

            log.info("[FOOD_SYNC_SCHEDULED] Найдено {} заведений для синхронизации", venueIds.size());

            int successCount = 0;
            int errorCount = 0;

            for (Long venueId : venueIds) {
                trySyncVenue(venueId, "SCHEDULED");
                
                if (foodSyncService.getSyncStatus(venueId) != null && 
                    foodSyncService.getSyncStatus(venueId).getErrorMessage() == null) {
                    successCount++;
                } else {
                    errorCount++;
                }
            }

            log.info("[FOOD_SYNC_SCHEDULED] Синхронизация завершена: успешно={}, с ошибками={}", 
                    successCount, errorCount);

        } catch (Exception e) {
            log.error("[FOOD_SYNC_SCHEDULED_ERROR] Критическая ошибка при синхронизации: {}", 
                    e.getMessage(), e);
        }
    }

    /**
     * Синхронизация заведения с защитой от параллельных запусков.
     */
    private void trySyncVenue(Long venueId, String syncType) {
        if (!syncingVenues.add(venueId)) {
            log.warn("[FOOD_SYNC_SCHEDULED] Синхронизация для venueId={} уже запущена, пропускаем", venueId);
            return;
        }

        try {
            log.info("[FOOD_SYNC_SCHEDULED] Синхронизация venueId={}, type={}", venueId, syncType);
            foodSyncService.syncMenu(venueId, syncType);
            log.info("[FOOD_SYNC_SCHEDULED] Успешно завершена для venueId={}", venueId);
        } catch (Exception e) {
            log.error("[FOOD_SYNC_SCHEDULED_ERROR] Ошибка синхронизации venueId={}: {}", 
                    venueId, e.getMessage(), e);
        } finally {
            syncingVenues.remove(venueId);
        }
    }
}
