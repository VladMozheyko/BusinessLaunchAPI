package dev.codelad.lunchservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ от Notification Service при создании уведомления.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private boolean success;
    private String notificationId;
    private String message;
}
