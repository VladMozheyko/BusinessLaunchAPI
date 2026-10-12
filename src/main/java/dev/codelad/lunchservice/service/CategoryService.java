package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.CreateCategoryRequest;
import dev.codelad.lunchservice.dto.response.CategoryResponse;
import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.exception.ResourceNotFoundException;
import dev.codelad.lunchservice.repository.CategoryRepository;
import dev.codelad.lunchservice.repository.VenueClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final VenueClient venueClient;

    @Value("${app.venue.check-enabled:false}")
    private boolean venueCheckEnabled;

    @Transactional
    public Category createCategory(CreateCategoryRequest categoryRequest) {
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
        category.setVenueId(categoryRequest.getVenueId());
        return categoryRepository.save(category);
    }

    /**
     * Получить все категории для заведения.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findByVenueId(Long venueId) {
        List<Category> categories = categoryRepository.findByVenueId(venueId);
        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(
                    "Категория с ID " + categoryId + " не найдена");
        }

        categoryRepository.deleteById(categoryId);
        log.info("[CATEGORY_DELETED] Категория {} удалена вместе с связанными блюдами", categoryId);
    }
}
