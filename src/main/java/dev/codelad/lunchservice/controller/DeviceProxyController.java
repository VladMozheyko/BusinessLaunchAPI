package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.DeviceRegistrationRequest;
import dev.codelad.lunchservice.dto.response.ErrorResponse;
import dev.codelad.lunchservice.repository.NotificationFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Прокси-контроллер для регистрации устройств.
 * 
 * Перенаправляет запросы клиента в Notification Service.
 * 
 * Как работает:
 * 1. Клиент (мобильное приложение) шлёт POST /api/devices/register-device с JWT
 * 2. LunchService валидирует JWT через JwtAuthenticationFilter
 * 3. Извлекает userId из SecurityContext
 * 4. Перенаправляет запрос в Notification Service через Feign Client
 * 5. Notification Service добавляет X-Internal-Service-Key
 * 6. Ответ возвращается клиенту
 * 
 * Почему так:
 * - Клиент не обращается напрямую к Notification Service
 * - Аутентификация через JWT LunchService
 * - Notification Service остаётся внутренним сервисом
 */
@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = ApiTags.DEVICES, description = ApiTags.DEVICES_DESC)
public class DeviceProxyController {

    private final NotificationFeignClient notificationFeignClient;

    /**
     * Зарегистрировать устройство пользователя.
     * 
     * Пример запроса:
     * POST /api/devices/register-device
     * Headers: Authorization: Bearer <JWT>
     * {
     *   "platform": "ANDROID",
     *   "fcmToken": "eWsxN4:APA91bHJk...",
     *   "timezone": "Europe/Moscow"
     * }
     */
    @PostMapping("/register-device")
    @Operation(summary = "Зарегистрировать устройство",
        description = "Проксирует запрос в Notification Service для регистрации FCM/APNs токена")
    public ResponseEntity<Object> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("[DEVICE_PROXY] Регистрируем устройство для пользователя");

        try {
            ResponseEntity<Map<String, Object>> response = 
                    notificationFeignClient.registerDevice(request);
            
            log.info("[DEVICE_PROXY] Устройство зарегистрировано успешно");
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("[DEVICE_PROXY] Ошибка при регистрации устройства: {}", e.getMessage(), e);
            ErrorResponse errorResponse = new ErrorResponse(
                    Instant.now().toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "INTERNAL_ERROR",
                    "Ошибка при регистрации устройства",
                    httpRequest.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
