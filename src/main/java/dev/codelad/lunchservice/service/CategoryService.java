package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.CreateCategoryRequest;
import dev.codelad.lunchservice.dto.response.CategoryFullResponse;
import dev.codelad.lunchservice.dto.response.CategoryResponse;
import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.entity.Dish;
import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.exception.ResourceNotFoundException;
import dev.codelad.lunchservice.exception.ValidationException;
import dev.codelad.lunchservice.repository.CategoryRepository;
import dev.codelad.lunchservice.repository.VenueClient;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final VenueClient venueClient;
    private final FoodSyncService foodSyncService;
    private final VenueProfileRepository venueProfileRepository;

    @Value("${app.venue.check-enabled:false}")
    private boolean venueCheckEnabled;

    /**
     * Создать категорию для заведения.
     */
    @Transactional
    public Category createCategory(CreateCategoryRequest categoryRequest) {
        // Находим VenueProfile по venueId
        VenueProfile venueProfile = venueProfileRepository.findByVenueId(categoryRequest.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Заведение с ID: " + categoryRequest.getVenueId() + " не найдено"));

        if (venueCheckEnabled) {
            try {
                venueClient.getVenue(categoryRequest.getVenueId());
            } catch (FeignException e) {
                log.error("[VENUE_CHECK_FAILED] Не удалось проверить venue {}: {}",
                        categoryRequest.getVenueId(), e.getMessage());
                throw new ResourceNotFoundException(
                        "Заведение с ID: " + categoryRequest.getVenueId() + " не существует или недоступно"
                );
            }
        } else {
            log.debug("[VENUE_CHECK] Проверка venue отключена (local/dev режим)");
        }

        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setVenueProfile(venueProfile);
        category.setHidden(false);
        return categoryRepository.save(category);
    }

    /**
     * Получить все категории для заведения с фильтрацией по hidden.
     * При первом запросе или если последняя синхронизация > 24 часов — запускает lazy sync.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findByVenueId(Long venueId, boolean includeHidden) {
        VenueProfile venueProfile = venueProfileRepository.findByVenueId(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Заведение с ID: " + venueId + " не найдено"));
        
        // Lazy sync: проверяем, нужна ли синхронизация
        triggerLazySyncIfNeeded(venueId);
        
        List<Category> categories = categoryRepository.findByVenueProfileAndIncludeHidden(venueProfile, includeHidden);
        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Получить все категории с блюдами для заведения.
     * При первом запросе или если последняя синхронизация > 24 часов — запускает lazy sync.
     */
    @Transactional(readOnly = true)
    public List<CategoryFullResponse.CategoryWithDishes> findAllWithDishes(
            Long venueId, boolean includeHiddenCategories, boolean includeHiddenDishes) {
        
        VenueProfile venueProfile = venueProfileRepository.findByVenueId(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Заведение с ID: " + venueId + " не найдено"));
        
        // Lazy sync: проверяем, нужна ли синхронизация
        triggerLazySyncIfNeeded(venueId);
        
        log.info("[CATEGORY_FULL] Получение всех категорий с блюдами для venueId={}, includeHiddenCategories={}, includeHiddenDishes={}",
                venueId, includeHiddenCategories, includeHiddenDishes);
        
        List<Category> categories = categoryRepository.findByVenueProfileWithDishesAndIncludeHidden(
                venueProfile, includeHiddenCategories);
        
        return categories.stream()
                .map(cat -> {
                    List<Dish> dishes = cat.getDishes() != null ? cat.getDishes() : List.of();
                    if (!includeHiddenDishes) {
                        dishes = dishes.stream()
                                .filter(d -> !Boolean.TRUE.equals(d.getHidden()))
                                .toList();
                    }
                    return new CategoryFullResponse.CategoryWithDishes(
                            cat.getId(),
                            cat.getName(),
                            cat.getVenueProfile().getVenueId(),
                            cat.getHidden(),
                            dishes
                    );
                })
                .toList();
    }

    /**
     * Запустить синхронизацию, если она нужна.
     * Запускается асинхронно, чтобы не блокировать запрос.
     */
    @Async("foodSyncExecutor")
    public void triggerLazySyncIfNeeded(Long venueId) {
        try {
            if (foodSyncService.needsSync(venueId)) {
                log.info("[LAZY_SYNC] Запуск lazy sync для venueId={}", venueId);
                foodSyncService.syncMenu(venueId, "LAZY");
                log.info("[LAZY_SYNC] Завершена для venueId={}", venueId);
            }
        } catch (Exception e) {
            log.warn("[LAZY_SYNC_WARN] Ошибка lazy sync для venueId={}: {}", venueId, e.getMessage());
        }
    }

    /**
     * Переключить статус видимости категории (toggle hidden).
     * Если скрыта → покажет, если скрыта → скроет.
     */
    @Transactional
    public Category toggleCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Категория с ID " + categoryId + " не найдена"));

        boolean newHiddenState = !Boolean.TRUE.equals(category.getHidden());
        category.setHidden(newHiddenState);
        Category saved = categoryRepository.save(category);
        
        if (newHiddenState) {
            log.info("[CATEGORY_HIDDEN] Категория {} скрыта", categoryId);
        } else {
            log.info("[CATEGORY_SHOWN] Категория {} показана", categoryId);
        }
        
        return saved;
    }

    /**
     * Физическое удаление категории (только GLOBAL_ADMIN).
     */
    @Transactional
    public void hardDeleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Категория с ID " + categoryId + " не найдена"));

        categoryRepository.delete(category);
        log.info("[CATEGORY_HARD_DELETED] Категория {} физически удалена", categoryId);
    }

    public List<Category> getAllCategories(Long venueId) {
        VenueProfile venueProfile = venueProfileRepository.findByVenueId(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Заведение с ID: " + venueId + " не найдено"));
        
        log.info("[CATEGORY_GET_ALL] Получение всех категорий для venueId={}", venueId);
        return categoryRepository.findByVenueProfile(venueProfile);
    }
}
