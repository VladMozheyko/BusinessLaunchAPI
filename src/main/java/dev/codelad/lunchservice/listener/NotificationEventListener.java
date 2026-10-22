package dev.codelad.lunchservice.listener;

import dev.codelad.lunchservice.dto.request.NotificationRequest;
import dev.codelad.lunchservice.dto.response.NotificationResponse;
import dev.codelad.lunchservice.repository.NotificationFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Слушатель событий уведомлений.
 * 
 * Как работает:
 * 1. NotificationEventPublisher публикует NotificationEvent
 * 2. Этот слушатель получает событие через @EventListener
 * 3. Метод handleNotificationEvent помечен @Async — выполняется в отдельном потоке
 * 4. Слушатель преобразует NotificationEvent в NotificationRequest
 * 5. Отправляет запрос через NotificationFeignClient к Notification Service
 * 
 * Почему @Async?
 * - Не блокируем основной поток обработки запроса
 * - Пользователь не ждёт, пока отправится push-уведомление
 * - Если Notification Service недоступен — не ломает основной функционал
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationFeignClient notificationFeignClient;

    /**
     * Обрабатывает событие отправки уведомления.
     * Выполняется асинхронно в отдельном потоке.
     *
     * @param event — событие с данными уведомления
     */
    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("[NOTIFICATION_LISTENER] Received event: type={}, userId={}", event.getEventType(), event.getUserId());

        try {
            // 1. Создаём запрос для Notification Service
            NotificationRequest request = buildNotificationRequest(event);

            // 2. Отправляем через Feign-клиент (публичный endpoint, без JWT)
            ResponseEntity<NotificationResponse> response =
                    notificationFeignClient.createNotification(request);

            // 3. Обрабатываем ответ
            if (response.getBody() != null && response.getBody().isSuccess()) {
                log.info("[NOTIFICATION_SENT] Success: type={}, userId={}, notificationId={}",
                        event.getEventType(),
                        event.getUserId(),
                        response.getBody().getNotificationId());
            } else {
                log.error("[NOTIFICATION_FAILED] Failed: type={}, userId={}, response={}",
                        event.getEventType(),
                        event.getUserId(),
                        response.getBody());
            }

        } catch (Exception e) {
            // Если Notification Service недоступен — логируем, но не бросаем исключение
            // Это не должно ломать основной функционал LunchService
            log.error("[NOTIFICATION_ERROR] Failed to send notification: type={}, userId={}, error={}",
                    event.getEventType(),
                    event.getUserId(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Создаёт NotificationRequest из NotificationEvent.
     * Преобразуем данные события в формат, понятный Notification Service.
     */
    private NotificationRequest buildNotificationRequest(NotificationEvent event) {
        // Преобразуем templateData в Map<String, String> для Notification Service
        Map<String, String> dataMap = new HashMap<>();
        if (event.getTemplateData() != null) {
            dataMap.putAll(event.getTemplateData());
        }

        return NotificationRequest.builder()
                .userId(event.getUserId())
                .notificationType(event.getEventType().name())
                .title(event.getTitle())
                .message(event.getMessage())
                .data(dataMap)
                .build();
    }
}
