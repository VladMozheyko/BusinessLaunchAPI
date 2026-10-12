package dev.codelad.lunchservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Запрос на отправку push-уведомления.
 * Совместим с SendNotificationRequest Notification Service.
 * 
 * Поля соответствуют SendNotificationRequest Notification Service
 * для корректной сериализации через Feign (JSON).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    /**
     * Заголовок уведомления.
     */
    private String title;

    /**
     * Текст уведомления (тело).
     */
    private String body;

    /**
     * Список UUID пользователей-получателей.
     */
    private List<UUID> userIds;

    /**
     * Канал уведомления: SYSTEM или MARKETING.
     */
    private String channel;

    /**
     * Высокий приоритет отправки.
     */
    @Builder.Default
    private boolean highPriority = false;

    /**
     * URL изображения для уведомления.
     */
    private String imageUrl;

    /**
     * Collapse key для группировки уведомлений.
     */
    private String collapseKey;

    /**
     * Дополнительные данные.
     */
    private Map<String, Object> data;

    /**
     * Создаёт SendNotificationRequest из NotificationEvent.
     * 
     * @param userId — UUID пользователя (String)
     * @param title — заголовок
     * @param message — текст
     * @param templateData — дополнительные данные
     * @return готовый запрос для Notification Service
     */
    public static SendNotificationRequest fromEvent(String userId, String title, String message, Map<String, String> templateData) {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setTitle(title);
        request.setBody(message != null ? message : "");
        
        List<UUID> userIds = new ArrayList<>();
        if (userId != null && !userId.isBlank()) {
            userIds.add(UUID.fromString(userId));
        }
        request.setUserIds(userIds);
        
        Map<String, Object> dataMap = new HashMap<>();
        if (templateData != null) {
            dataMap.putAll(templateData);
        }
        request.setData(dataMap);
        
        request.setChannel("SYSTEM");
        
        return request;
    }
}
