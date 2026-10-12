package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.dto.request.DeviceRegistrationRequest;
import dev.codelad.lunchservice.dto.request.SendNotificationRequest;
import dev.codelad.lunchservice.dto.response.NotificationResponse;
import dev.codelad.lunchservice.dto.request.PreferencesRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign-клиент для вызовов Notification Service.
 * 
 * Как работает:
 * - name = "notification-service" — имя сервиса в Kubernetes DNS / Service Discovery
 * - path = "/api/public" — базовый путь к публичным endpoint-ам Notification Service
 * - url — переопределяет имя сервиса для локальной разработки
 * 
 * lunch-service -> notification-service:8082/api/public/send
 * 
 * Endpoint /api/public/send — требует заголовок X-Internal-Service-Key (добавляется FeignConfig).
 */
@FeignClient(
        name = "notification-service",
        path = "/api/public",
        url = "${notification.service.url:http://localhost:8082}"
)
public interface NotificationFeignClient {

    /**
     * Отправить push-уведомление.
     * 
     * Notification Service сам найдёт устройства пользователя и отправит push.
     * 
     * @param request — тело запроса с данными уведомления
     * @return ответ от Notification Service
     */
    @PostMapping("/send")
    ResponseEntity<NotificationResponse> sendNotification(
            @RequestBody SendNotificationRequest request
    );

    /**
     * Зарегистрировать устройство пользователя.
     * 
     * @param request — данные устройства (platform, fcmToken, timezone)
     * @return результат регистрации
     */
    @PostMapping("/devices")
    ResponseEntity<Map<String, Object>> registerDevice(
            @RequestBody DeviceRegistrationRequest request
    );

    /**
     * Обновить настройки уведомлений пользователя.
     * 
     * @param request — настройки (pushEnabled, marketing)
     * @return результат обновления
     */
    @PostMapping("/notifications")
    ResponseEntity<Map<String, Object>> updatePreferences(
            @RequestBody PreferencesRequest request
    );

    /**
     * Получить статус уведомления по ID.
     * 
     * @param id — ID уведомления
     * @return статус и метрики уведомления
     */
    @GetMapping("/notifications/{id}/status")
    ResponseEntity<NotificationResponse> getNotificationStatus(
            @PathVariable("id") Long id
    );
}
