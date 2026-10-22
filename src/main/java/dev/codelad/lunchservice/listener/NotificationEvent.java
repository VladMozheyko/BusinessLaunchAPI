package dev.codelad.lunchservice.listener;

import dev.codelad.lunchservice.enums.NotificationEventType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Spring Event для отправки уведомлений.
 * 
 * Как работает:
 * 1. LunchBookingService публикует NotificationEvent (через NotificationEventPublisher)
 * 2. NotificationEventListener получает событие и асинхронно отправляет push через Notification Service
 * 
 * Это чистая архитектура — LunchService не знает про Notification Service напрямую.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class NotificationEvent extends ApplicationEvent {

    /**
     * UUID пользователя (из JWT токена, externalId)
     */
    private final String userId;

    /**
     * Тип события (BOOKING_CREATED, BOOKING_CANCELLED, etc.)
     */
    private final NotificationEventType eventType;

    /**
     * Заголовок уведомления (будет заполнен шаблоном)
     */
    private final String title;

    /**
     * Текст уведомления (будет заполнен шаблоном)
     */
    private final String message;

    /**
     * Дополнительные данные для шаблона:
     * - venueName — название ресторана
     * - bookingId — ID брони
     * - time — время брони
     * - и другие
     */
    private final Map<String, String> templateData;

    /**
     * JWT токен пользователя (для авторизации в Notification Service)
     * Не используется для публичного endpoint, но может понадобиться в будущем.
     */
    private final String jwtToken;

    /**
     * Создаёт событие для LunchBooking
     *
     * @param userId — UUID пользователя
     * @param eventType — тип события
     * @param title — заголовок
     * @param message — текст
     * @param templateData — данные для шаблона
     * @param jwtToken — JWT токен
     */
    public static NotificationEvent forLunchBooking(
            String userId,
            NotificationEventType eventType,
            String title,
            String message,
            Map<String, String> templateData,
            String jwtToken
    ) {
        return NotificationEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .title(title)
                .message(message)
                .templateData(templateData)
                .jwtToken(jwtToken)
                .build();
    }

    /**
     * Создаёт событие для ComboBooking
     */
    public static NotificationEvent forComboBooking(
            String userId,
            NotificationEventType eventType,
            String title,
            String message,
            Map<String, String> templateData,
            String jwtToken
    ) {
        return NotificationEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .title(title)
                .message(message)
                .templateData(templateData)
                .jwtToken(jwtToken)
                .build();
    }

    /**
     * Конструктор для ApplicationEvent.
     * Spring требует source в конструкторе.
     */
    public NotificationEvent(
            String userId,
            NotificationEventType eventType,
            String title,
            String message,
            Map<String, String> templateData,
            String jwtToken
    ) {
        super(new Object()); // Spring Event требует source
        this.userId = userId;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.templateData = templateData;
        this.jwtToken = jwtToken;
    }
}
