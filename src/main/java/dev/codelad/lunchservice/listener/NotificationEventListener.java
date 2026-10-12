package dev.codelad.lunchservice.listener;

import dev.codelad.lunchservice.dto.request.SendNotificationRequest;
import dev.codelad.lunchservice.dto.response.NotificationResponse;
import dev.codelad.lunchservice.repository.NotificationFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;

/**
 * Слушатель событий уведомлений.
 * 
 * Как работает:
 * 1. NotificationEventPublisher публикует NotificationEvent
 * 2. Этот слушатель получает событие через @EventListener
 * 3. Метод handleNotificationEvent помечен @Async — выполняется в отдельном потоке
 * 4. Слушатель преобразует NotificationEvent в SendNotificationRequest
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
            SendNotificationRequest request = SendNotificationRequest.fromEvent(
                    event.getUserId(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getTemplateData()
            );

            // 2. Отправляем через Feign-клиент (требует X-Internal-Service-Key header)
            ResponseEntity<NotificationResponse> response = notificationFeignClient.sendNotification(request);

            // 3. Обрабатываем ответ
            if (response.getBody() != null) {
                NotificationResponse body = response.getBody();
                log.info("[NOTIFICATION_SENT] Success: type={}, userId={}, notificationId={}, status={}",
                        event.getEventType(),
                        event.getUserId(),
                        body.getId(),
                        body.getStatus());
            } else {
                log.error("[NOTIFICATION_FAILED] Failed: type={}, userId={}, response body is null",
                        event.getEventType(),
                        event.getUserId());
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
}
