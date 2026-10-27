package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.foodservice.FoodServiceCategoryDTO;
import dev.codelad.lunchservice.dto.foodservice.FoodServiceDishDTO;
import dev.codelad.lunchservice.dto.foodservice.FoodServiceMenuDTO;
import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.entity.Dish;
import dev.codelad.lunchservice.entity.FoodSyncStatus;
import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.enums.DishType;
import dev.codelad.lunchservice.repository.CategoryRepository;
import dev.codelad.lunchservice.repository.DishRepository;
import dev.codelad.lunchservice.repository.FoodServiceFeignClient;
import dev.codelad.lunchservice.repository.FoodSyncStatusRepository;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для синхронизации категорий и блюд из Food Service (кассовый аппарат).
 * 
 * Стратегия разрешения конфликтов:
 * - Если локальная категория/блюдо создано пользователем (syncedFromFood=false) → оставляем локальные данные
 * - Если категория/блюдо синхронизировано (syncedFromFood=true) → обновляем данные из Food Service
 * - Если не существует локально → создаём из Food Service с syncedFromFood=true и hidden=true
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodSyncService {

    private final FoodServiceFeignClient foodServiceFeignClient;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final VenueProfileRepository venueProfileRepository;
    private final FoodSyncStatusRepository foodSyncStatusRepository;

    /**
     * Синхронизировать меню заведения из Food Service.
     * Вызывается при входе BAR_OWNER/BREW_OWNER или по запросу через контроллер.
     *
     * @param venueId ID заведения
     * @param syncType тип синхронизации: SCHEDULED, MANUAL, LAZY
     * @return количество синхронизированных категорий и блюд
     */
    @Transactional
    public FoodSyncResult syncMenu(Long venueId, String syncType) {
        log.info("Начало синхронизации меню для venueId={}, type={}", venueId, syncType);

        // Получаем данные из Food Service
        ResponseEntity<FoodServiceMenuDTO> response;
        try {
            response = foodServiceFeignClient.getMenu(venueId);
        } catch (Exception e) {
            log.error("Ошибка получения данных из Food Service для venueId={}: {}", venueId, e.getMessage(), e);
            saveSyncStatus(venueId, syncType, 0, 0, 0, "Ошибка подключения к Food Service: " + e.getMessage());
            return new FoodSyncResult(0, 0, 0, "Ошибка подключения к Food Service: " + e.getMessage());
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Food Service вернул пустой ответ для venueId={}", venueId);
            saveSyncStatus(venueId, syncType, 0, 0, 0, "Food Service не вернул данные");
            return new FoodSyncResult(0, 0, 0, "Food Service не вернул данные");
        }

        FoodServiceMenuDTO menuDTO = response.getBody();

        // Обновляем логотип (если есть) — работает и для scheduled sync, и для manual sync
        if (menuDTO.getLogoUrl() != null) {
            updateLogo(venueId, menuDTO.getLogoUrl());
        }

        // Находим VenueProfile
        VenueProfile venueProfile = venueProfileRepository.findByVenueId(venueId)
                .orElseThrow(() -> new RuntimeException("Заведение с ID: " + venueId + " не найдено"));

        // Синхронизируем категории
        Map<String, Category> existingCategoriesByName = categoryRepository
                .findByVenueProfile(venueProfile)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getName().toLowerCase(),
                        c -> c,
                        (existing, replacement) -> existing
                ));

        int categoriesCreated = 0;
        int categoriesUpdated = 0;

        for (FoodServiceCategoryDTO categoryDTO : menuDTO.getCategories()) {
            String categoryName = categoryDTO.getName();
            
            if (existingCategoriesByName.containsKey(categoryName.toLowerCase())) {
                Category existingCategory = existingCategoriesByName.get(categoryName.toLowerCase());
                
                if (Boolean.TRUE.equals(existingCategory.getSyncedFromFood())) {
                    // Синхронизированная категория → обновляем
                    existingCategory.setUpdatedAt(Instant.now());
                    categoryRepository.save(existingCategory);
                    categoriesUpdated++;
                    log.debug("Обновлена синхронизированная категория: {}", categoryName);
                } else {
                    // Локальная категория → оставляем как есть
                    log.debug("Пропущена локальная категория: {}", categoryName);
                }
            } else {
                // Создаём новую категорию из Food Service
                Category newCategory = Category.builder()
                        .venueProfile(venueProfile)
                        .name(categoryName)
                        .hidden(true)
                        .syncedFromFood(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                
                categoryRepository.save(newCategory);
                categoriesCreated++;
                log.debug("Создана новая синхронизированная категория: {}", categoryName);
            }
        }

        // Синхронизируем блюда
        Map<String, Dish> existingDishesByName = dishRepository
                .findAll()
                .stream()
                .filter(d -> d.getCategory() != null && d.getCategory().getVenueProfile().getVenueId().equals(venueId))
                .collect(Collectors.toMap(
                        d -> d.getName().toLowerCase(),
                        d -> d,
                        (existing, replacement) -> existing
                ));

        int dishesCreated = 0;
        int dishesUpdated = 0;

        for (FoodServiceDishDTO dishDTO : menuDTO.getDishes()) {
            String dishName = dishDTO.getName();
            
            if (existingDishesByName.containsKey(dishName.toLowerCase())) {
                Dish existingDish = existingDishesByName.get(dishName.toLowerCase());
                
                if (Boolean.TRUE.equals(existingDish.getSyncedFromFood())) {
                    // Синхронизированное блюдо → обновляем данные
                    updateDishFromFoodService(existingDish, dishDTO);
                    dishRepository.save(existingDish);
                    dishesUpdated++;
                    log.debug("Обновлено синхронизированное блюдо: {}", dishName);
                } else {
                    // Локальное блюдо → оставляем как есть
                    log.debug("Пропущено локальное блюдо: {}", dishName);
                }
            } else {
                // Создаём новое блюдо из Food Service
                Dish newDish = createDishFromFoodService(venueId, dishDTO);
                if (newDish != null) {
                    dishRepository.save(newDish);
                    dishesCreated++;
                    log.debug("Создано новое синхронизированное блюдо: {}", dishName);
                }
            }
        }

        // Сохраняем статус синхронизации
        saveSyncStatus(venueId, syncType, 
                categoriesCreated + categoriesUpdated, 
                dishesCreated + dishesUpdated,
                categoriesCreated + dishesCreated,
                null);

        log.info("Синхронизация меню для venueId={} завершена успешно: {} категорий создано, {} обновлено, {} блюд создано, {} обновлено",
                venueId, categoriesCreated, categoriesUpdated, dishesCreated, dishesUpdated);

        return new FoodSyncResult(
                categoriesCreated + categoriesUpdated,
                dishesCreated + dishesUpdated,
                categoriesCreated + dishesCreated,
                null
        );
    }

    /**
     * Проверить, нужна ли синхронизация для заведения.
     * Возвращает true, если с последней синхронизации прошло > 24 часов.
     */
    public boolean needsSync(Long venueId) {
        return foodSyncStatusRepository.findByVenueId(venueId).map(status -> {
            if (status.getLastSyncTime() == null) {
                return true;
            }
            Duration duration = Duration.between(status.getLastSyncTime(), Instant.now());
            boolean needs = duration.toHours() >= 24;
            if (needs) {
                log.info("Заведение venueId={} требует синхронизации (последняя: {} часов назад)", 
                        venueId, duration.toHours());
            }
            return needs;
        }).orElse(true); // Если нет статуса — нужна синхронизация
    }

    /**
     * Получить статус синхронизации для заведения.
     */
    public FoodSyncStatus getSyncStatus(Long venueId) {
        return foodSyncStatusRepository.findByVenueId(venueId)
                .orElse(null);
    }

    /**
     * Синхронизировать только логотип из Food Service.
     * Вызывается по отдельному endpoint /sync-logo/{venueId}.
     * 
     * @param venueId ID заведения
     * @return true если логотип успешно обновлён
     */
    @Transactional
    public boolean syncLogoOnly(Long venueId) {
        try {
            ResponseEntity<FoodServiceMenuDTO> response = foodServiceFeignClient.getMenu(venueId);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Food Service вернул пустой ответ для venueId={}", venueId);
                return false;
            }

            FoodServiceMenuDTO menuDTO = response.getBody();
            
            if (menuDTO.getLogoUrl() == null) {
                log.info("Food Service не вернул логотип для venueId={}", venueId);
                return false;
            }

            updateLogo(venueId, menuDTO.getLogoUrl());
            return true;
            
        } catch (Exception e) {
            log.error("Ошибка синхронизации логотипа для venueId={}: {}", venueId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Обновить логотип заведения.
     * Вызывается при sync (scheduled, manual, lazy) и при запросе /venue/my.
     */
    @Transactional
    public void updateLogo(Long venueId, String logoUrl) {
        venueProfileRepository.findByVenueId(venueId).ifPresent(profile -> {
            // Обновляем только если логотип изменился
            if (!logoUrl.equals(profile.getLogoUrl())) {
                profile.setLogoUrl(logoUrl);
                profile.setUpdatedAt(Instant.now());
                venueProfileRepository.save(profile);
                log.info("[VENUE_LOGO] Логотип обновлён для venueId={}: {}", venueId, logoUrl);
            } else {
                log.debug("[VENUE_LOGO] Логотип не изменился для venueId={}", venueId);
            }
        });
    }

    /**
     * Обновить данные блюда из Food Service.
     */
    private void updateDishFromFoodService(Dish dish, FoodServiceDishDTO dishDTO) {
        if (dishDTO.getPrice() != null) {
            dish.setPrice(dishDTO.getPrice());
        }
        if (dishDTO.getWeight() != null) {
            dish.setWeight(dishDTO.getWeight());
        }
        if (dishDTO.getWeightUnit() != null) {
            dish.setWeightUnit(dishDTO.getWeightUnit());
        }
        if (dishDTO.getAvailable() != null) {
            dish.setAvailable(dishDTO.getAvailable());
        }
        if (dishDTO.getDishType() != null) {
            try {
                dish.setDishType(DishType.fromValue(dishDTO.getDishType()));
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный тип блюда из Food Service: {}, используем MAIN", dishDTO.getDishType());
                dish.setDishType(DishType.MAIN);
            }
        }
        dish.setUpdatedAt(Instant.now());
    }

    /**
     * Создать блюдо из данных Food Service.
     */
    private Dish createDishFromFoodService(Long venueId, FoodServiceDishDTO dishDTO) {
        // Находим VenueProfile
        VenueProfile venueProfile = venueProfileRepository.findByVenueId(venueId)
                .orElseThrow(() -> new RuntimeException("Заведение с ID: " + venueId + " не найдено"));

        // Находим категорию по имени
        List<Category> categories = categoryRepository.findByVenueProfile(venueProfile);
        Category category = categories.stream()
                .filter(c -> c.getName().equals(dishDTO.getCategoryId()))
                .findFirst()
                .orElse(null);

        if (category == null) {
            log.warn("Категория не найдена для блюда {}: {}", dishDTO.getName(), dishDTO.getCategoryId());
            return null;
        }

        return Dish.builder()
                .category(category)
                .name(dishDTO.getName())
                .weight(dishDTO.getWeight() != null ? dishDTO.getWeight() : 0)
                .weightUnit(dishDTO.getWeightUnit() != null ? dishDTO.getWeightUnit() : "г")
                .price(dishDTO.getPrice() != null ? dishDTO.getPrice() : 0)
                .isAvailable(dishDTO.getAvailable() != null ? dishDTO.getAvailable() : false)
                .dishType(parseDishType(dishDTO.getDishType()))
                .hidden(true)
                .syncedFromFood(true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Безопасно парсит тип блюда из строки.
     */
    private DishType parseDishType(String dishTypeValue) {
        if (dishTypeValue == null || dishTypeValue.isBlank()) {
            return DishType.MAIN;
        }
        try {
            return DishType.fromValue(dishTypeValue);
        } catch (IllegalArgumentException e) {
            log.warn("Неизвестный тип блюда из Food Service: {}, используем MAIN", dishTypeValue);
            return DishType.MAIN;
        }
    }

    /**
     * Сохранить статус синхронизации.
     */
    private void saveSyncStatus(Long venueId, String syncType, 
                                 int categoriesCount, int dishesCount, 
                                 int newlyCreated, String error) {
        Instant now = Instant.now();
        
        FoodSyncStatus status = foodSyncStatusRepository.findByVenueId(venueId)
                .orElse(FoodSyncStatus.builder()
                        .venueId(venueId)
                        .build());

        status.setLastSyncTime(now);
        status.setSyncType(syncType);
        status.setSyncedCategoriesCount(categoriesCount);
        status.setSyncedDishesCount(dishesCount);
        status.setErrorMessage(error);
        
        foodSyncStatusRepository.save(status);
        
        if (error == null) {
            log.info("Статус синхронизации для venueId={} сохранён: {} категорий, {} блюд, newlyCreated={}",
                    venueId, categoriesCount, dishesCount, newlyCreated);
        } else {
            log.warn("Статус синхронизации для venueId={} сохранён с ошибкой: {}", venueId, error);
        }
    }

    /**
     * Результат синхронизации меню.
     */
    public record FoodSyncResult(
            int totalSynced,
            int dishesSynced,
            int newlyCreated,
            String error
    ) {
        public boolean isSuccess() {
            return error == null;
        }
    }
}
