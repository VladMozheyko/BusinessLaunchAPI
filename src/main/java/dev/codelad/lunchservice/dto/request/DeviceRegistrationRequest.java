package dev.codelad.lunchservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Запрос на регистрацию устройства.
 * Проксирует запрос к Notification Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    /**
     * UUID пользователя (из JWT LunchService).
     */
    private UUID userId;

    /**
     * ID существующего устройства для обновления (опционально)
     */
    private Long deviceId;

    /**
     * Платформа устройства: ANDROID, IOS, WEB
     */
    private String platform;

    /**
     * FCM/APNs/Web Push токен
     */
    private String fcmToken;

    /**
     * Часовой пояс пользователя (опционально)
     */
    private String timezone;
}
