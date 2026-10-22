package dev.codelad.lunchservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Запрос на обновление настроек уведомлений.
 * Проксирует запрос к Notification Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesRequest {

    /**
     * UUID пользователя (из JWT LunchService).
     */
    private UUID userId;

    /**
     * Включены ли push-уведомления
     */
    private Boolean pushEnabled;

    /**
     * Получать ли маркетинговые уведомления
     */
    private Boolean marketing;
}
