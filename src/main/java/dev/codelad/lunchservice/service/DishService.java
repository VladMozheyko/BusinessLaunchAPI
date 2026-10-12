package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.request.CreateDishRequest;
import dev.codelad.lunchservice.dto.request.UpdateAvailabilityRequest;
import dev.codelad.lunchservice.dto.request.UpdateDishRequest;
import dev.codelad.lunchservice.dto.request.UpdateFreeAddonRequest;
import dev.codelad.lunchservice.dto.response.DishResponse;
import dev.codelad.lunchservice.entity.Dish;
import dev.codelad.lunchservice.exception.DishNotFoundException;
import dev.codelad.lunchservice.exception.ResourceNotFoundException;
import dev.codelad.lunchservice.exception.ValidationException;
import dev.codelad.lunchservice.repository.CategoryRepository;
import dev.codelad.lunchservice.repository.DishRepository;
import dev.codelad.lunchservice.repository.LunchBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final LunchBookingRepository lunchBookingRepository;

    @Transactional
    public Dish createDish(CreateDishRequest request) {
        // Проверка существования категории
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResourceNotFoundException(
                    "Категория с ID " + request.getCategoryId() + " не найдена");
        }

        Dish dish = Dish.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .price(request.getPrice())
                .isAvailable(request.getIsAvailable() != null && request.getIsAvailable())
                .quantity(request.getQuantity())
                .isFreeAddon(request.getIsFreeAddon() != null && request.getIsFreeAddon())
                .build();

        return dishRepository.save(dish);
    }

    /**
     * Получить все блюда для категории.
     */
    @Transactional(readOnly = true)
    public List<DishResponse> findByCategoryId(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(
                    "Категория с ID " + categoryId + " не найдена");
        }

        List<Dish> dishes = dishRepository.findByCategoryId(categoryId);
        return dishes.stream()
                .map(DishResponse::from)
                .toList();
    }

    /**
     * Получить все блюда для заведения (через категории).
     */
    @Transactional(readOnly = true)
    public List<DishResponse> findByVenueId(Long venueId) {
        List<Dish> dishes = dishRepository.findByVenueId(venueId);
        return dishes.stream()
                .map(DishResponse::from)
                .toList();
    }

    @Transactional
    public Dish updateDish(UUID dishId, UpdateDishRequest request) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new DishNotFoundException(
                        noIdMessage(dishId)));

        // Проверяем, есть ли активные заказы с этим блюдом
        if (lunchBookingRepository.existsByDishIdAndActiveOrders(dishId)) {
            throw new ValidationException(
                    "Нельзя изменить блюдо: есть активные бронирования. " +
                    "Доступно только изменение статуса (доступен/недоступен)." +
                    " После завершения заказов можно будет изменить название, вес и другие параметры.");
        }

        if (request.getWeight() != null) {
            dish.setWeight(request.getWeight());
        }
        if (request.getWeightUnit() != null) {
            dish.setWeightUnit(request.getWeightUnit());
        }
        if (request.getPrice() != null) {
            dish.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            dish.setQuantity(request.getQuantity());
        }

        log.info("[DISH_UPDATE] Блюдо {} обновлено", dishId);
        return dishRepository.save(dish);
    }

    @Transactional
    public Dish updateAvailability(UUID dishId, UpdateAvailabilityRequest request) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new DishNotFoundException(
                        noIdMessage(dishId)));

        boolean previousValue = dish.isAvailable();
        dish.setAvailable(request.getIsAvailable() != null && request.getIsAvailable());

        log.info("[DISH_UPDATE_AVAILABILITY] Блюдо {} изменено: isAvailable {} -> {}",
                dishId, previousValue, request.getIsAvailable() != null && request.getIsAvailable());

        return dishRepository.save(dish);
    }

    @Transactional
    public Dish updateFreeAddon(UUID dishId, UpdateFreeAddonRequest request) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new DishNotFoundException(
                        noIdMessage(dishId)));

        boolean previousValue = dish.isFreeAddon();
        dish.setFreeAddon(request.getIsFreeAddon() != null && request.getIsFreeAddon());

        log.info("[DISH_UPDATE_FREE_ADDON] Блюдо {} изменено: isFreeAddon {} -> {}",
                dishId, previousValue, request.getIsFreeAddon() != null && request.getIsFreeAddon());

        return dishRepository.save(dish);
    }

    @Transactional
    public void deleteDish(UUID dishId) {
        if (!dishRepository.existsById(dishId)) {
            throw new DishNotFoundException(
                    noIdMessage(dishId));
        }

        dishRepository.deleteById(dishId);
        log.info("[DISH_DELETED] Блюдо {} удалено", dishId);
    }

    private static String noIdMessage(UUID dishId) {
        return "Блюдо с ID " + dishId + " не найдено";
    }
}
