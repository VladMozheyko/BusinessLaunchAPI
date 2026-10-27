package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.dto.foodservice.FoodServiceMenuDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign клиент для получения данных из Food Service (кассовый аппарат).
 * Получает категории, блюда и логотип заведения.
 */
@Repository
@FeignClient(
        name = "food-service",
        path = "${food.service.path:/api/v1/food}",
        url = "${food.service.url:http://localhost:8086}"
)
public interface FoodServiceFeignClient {

    /**
     * Получить меню заведения (категории, блюда, логотип) из Food Service.
     *
     * @param venueId ID заведения в нашей системе
     * @return меню с категориями и блюдами
     */
    @GetMapping("/menu/{venueId}")
    ResponseEntity<FoodServiceMenuDTO> getMenu(@PathVariable("venueId") Long venueId);
}
