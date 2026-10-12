package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.CreateComboRequest;
import dev.codelad.lunchservice.dto.request.UpdateComboStatusRequest;
import dev.codelad.lunchservice.dto.response.ComboFullResponse;
import dev.codelad.lunchservice.dto.response.ComboResponse;
import dev.codelad.lunchservice.entity.Category;
import dev.codelad.lunchservice.entity.Combo;
import dev.codelad.lunchservice.entity.Dish;
import dev.codelad.lunchservice.exception.ResourceNotFoundException;
import dev.codelad.lunchservice.repository.CategoryRepository;
import dev.codelad.lunchservice.repository.ComboRepository;
import dev.codelad.lunchservice.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;

    @Transactional
    public Combo createCombo(CreateComboRequest request) {
        // Загружаем категории по ID
        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
        
        if (categories.size() != request.getCategoryIds().size()) {
            List<Long> foundIds = categories.stream().map(Category::getId).toList();
            List<Long> missingIds = request.getCategoryIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ResourceNotFoundException(
                    "Категории не найдены: " + missingIds);
        }

        // Загружаем бесплатные дополнения (если указаны)
        List<Dish> freeAddons = new ArrayList<>();
        if (request.getFreeAddonDishIds() != null && !request.getFreeAddonDishIds().isEmpty()) {
            freeAddons = dishRepository.findAllById(request.getFreeAddonDishIds());
            
            if (freeAddons.size() != request.getFreeAddonDishIds().size()) {
                List<UUID> foundIds = freeAddons.stream().map(Dish::getId).toList();
                List<UUID> missingIds = request.getFreeAddonDishIds().stream()
                        .filter(id -> !foundIds.contains(id))
                        .toList();
                throw new ResourceNotFoundException(
                        "Бесплатные дополнения не найдены: " + missingIds);
            }
        }

        Combo combo = Combo.builder()
                .venueId(request.getVenueId())
                .name(request.getName())
                .price(request.getPrice())
                .dailyLimit(request.getDailyLimit())
                .isActive(request.getIsActive() != null && request.getIsActive())
                .categories(categories)
                .freeAddons(freeAddons)
                .build();

        return comboRepository.save(combo);
    }

    /**
     * Получить все комбо для заведения.
     */
    @Transactional(readOnly = true)
    public List<ComboResponse> findByVenueId(Long venueId) {
        List<Combo> combos = comboRepository.findByVenueId(venueId);
        return combos.stream()
                .map(ComboResponse::from)
                .toList();
    }

    /**
     * Получить все комбо с деталями (категории, блюда, дополнения).
     * Для публичного доступа.
     */
    @Transactional(readOnly = true)
    public List<ComboFullResponse> findAllWithDetails() {
        List<Combo> combos = comboRepository.findAll();
        return combos.stream()
                .map(ComboFullResponse::from)
                .toList();
    }

    /**
     * Получить одно комбо с деталями по ID.
     * Для публичного доступа.
     */
    @Transactional(readOnly = true)
    public ComboFullResponse findByIdWithDetails(Long comboId) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Комбо с ID " + comboId + " не найдено"));
        return ComboFullResponse.from(combo);
    }

    @Transactional
    public Combo updateComboStatus(Long comboId, UpdateComboStatusRequest request) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Комбо с ID " + comboId + " не найдено"));

        boolean previousValue = combo.isActive();
        combo.setActive(request.getIsActive() != null && request.getIsActive());

        log.info("[COMBO_STATUS_UPDATE] Комбо {} изменено: isActive {} -> {}",
                comboId, previousValue, request.getIsActive() != null && request.getIsActive());

        return comboRepository.save(combo);
    }

    /**
     * Удалить комбо по ID.
     */
    @Transactional
    public void deleteCombo(Long comboId) {
        if (!comboRepository.existsById(comboId)) {
            throw new ResourceNotFoundException(
                    "Комбо с ID " + comboId + " не найдено");
        }
        comboRepository.deleteById(comboId);
        log.info("[COMBO_DELETED] Комбо {} удалено", comboId);
    }
}
