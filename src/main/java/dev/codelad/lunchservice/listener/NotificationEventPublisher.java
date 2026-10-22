package dev.codelad.lunchservice.listener;

import dev.codelad.lunchservice.enums.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Публикатор событий уведомлений.
 * 
 * Как работает:
 * 1. LunchBookingService вызывает notificationEventPublisher.publish(...)
 * 2. EventPublisher отправляет NotificationEvent всем слушателям (NotificationEventListener)
 * 3. NotificationEventListener обрабатывает событие асинхронно
 * 
 * LunchService НЕ знает про Notification Service — он просто публикует события.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Публикует событие создания брони обеда
     */
    public void publishLunchBookingEvent(
            String userId,
            NotificationEventType eventType,
            String title,
            String message,
            Map<String, String> templateData,
            String jwtToken
    ) {
        NotificationEvent event = new NotificationEvent(
                userId,
                eventType,
                title,
                message,
                templateData,
                jwtToken
        );

        eventPublisher.publishEvent(event);

        log.info("[NOTIFICATION_EVENT] Published event: type={}, userId={}", eventType, userId);
    }

    /**
     * Публикует событие создания брони комбо-набора
     */
    public void publishComboBookingEvent(
            String userId,
            NotificationEventType eventType,
            String title,
            String message,
            Map<String, String> templateData,
            String jwtToken
    ) {
        NotificationEvent event = new NotificationEvent(
                userId,
                eventType,
                title,
                message,
                templateData,
                jwtToken
        );

        eventPublisher.publishEvent(event);

        log.info("[NOTIFICATION_EVENT] Published event: type={}, userId={}", eventType, userId);
    }
}
