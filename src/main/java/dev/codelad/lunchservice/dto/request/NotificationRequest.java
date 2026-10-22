package dev.codelad.lunchservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Запрос на создание уведомления через Notification Service.
 * Содержит информацию о пользователе, типе уведомления, тексте и дополнительных данных.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    /**
     * UUID пользователя (из JWT токена, externalId)
     */
    private String userId;

    /**
     * Тип уведомления (например: "BOOKING_CREATED", "BOOKING_CANCELLED", "BOOKING_APPROVED")
     */
    private String notificationType;

    /**
     * Заголовок уведомления (отображается в push-уведомлении)
     */
    private String title;

    /**
     * Текст уведомления (отображается в push-уведомлении)
     */
    private String message;

    /**
     * Дополнительные данные для уведомления (опционально)
     * Например: бронь ID, время, название ресторана
     */
    private Map<String, String> data;
}
