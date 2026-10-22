package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.PreferencesRequest;
import dev.codelad.lunchservice.repository.NotificationFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Прокси-контроллер для управления настройками уведомлений.
 * 
 * Перенаправляет запросы клиента в Notification Service.
 * 
 * Как работает:
 * 1. Клиент (мобильное приложение) шлёт POST /api/notifications/preferences с JWT
 * 2. LunchService валидирует JWT через JwtAuthenticationFilter
 * 3. Извлекает userId из SecurityContext
 * 4. Перенаправляет запрос в Notification Service через Feign Client
 * 5. Notification Service обновляет user_preferences в БД
 * 6. Ответ возвращается клиенту
 * 
 * Почему так:
 * - Клиент не обращается напрямую к Notification Service
 * - Аутентификация через JWT LunchService
 * - Notification Service остаётся внутренним сервисом
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = ApiTags.PREFERENCES, description = ApiTags.PREFERENCES_DESC)
public class PreferencesProxyController {

    private final NotificationFeignClient notificationFeignClient;

    /**
     * Обновить настройки уведомлений.
     * 
     * Пример запроса:
     * POST /api/notifications/preferences
     * Headers: Authorization: Bearer <JWT>
     * {
     *   "pushEnabled": true,
     *   "marketing": false
     * }
     */
    @PostMapping("/preferences")
    @Operation(summary = "Обновить настройки уведомлений",
        description = "Проксирует запрос в Notification Service для обновления настроек push")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @Valid @RequestBody PreferencesRequest request) {
        
        log.info("[PREFERENCES_PROXY] Обновляем настройки уведомлений");

        try {
            ResponseEntity<Map<String, Object>> response = 
                    notificationFeignClient.updatePreferences(request);
            
            log.info("[PREFERENCES_PROXY] Настройки обновлены успешно");
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("[PREFERENCES_PROXY] Ошибка при обновлении настроек: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "INTERNAL_ERROR",
                "message", "Ошибка при обновлении настроек"
            ));
        }
    }
}
