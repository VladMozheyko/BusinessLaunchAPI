package dev.codelad.lunchservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Внутренний запрос на создание уведомления.
 * Используется NotificationEventPublisher и NotificationEvent.
 * 
 * Для отправки в Notification Service преобразуется в SendNotificationRequest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    /**
     * UUID пользователя (из JWT токена, externalId).
     */
    private String userId;

    /**
     * Заголовок уведомления.
     */
    private String title;

    /**
     * Текст уведомления.
     */
    private String message;

    /**
     * Дополнительные данные для шаблона.
     */
    private Map<String, String> data;
}
