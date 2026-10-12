package dev.codelad.lunchservice.dto.response;

import lombok.Data;

import java.time.Instant;

/**
 * Ответ от Notification Service о статусе уведомления.
 * Совместим с NotificationResponse Notification Service.
 */
@Data
public class NotificationResponse {

    /**
     * ID уведомления.
     */
    private Long id;

    /**
     * Статус уведомления: DRAFT, RESOLVING, READY, PROCESSING, SENT, PARTIAL, FAILED.
     */
    private String status;

    /**
     * Общее количество получателей.
     */
    private Integer totalRecipients;

    /**
     * Количество успешно отправленных.
     */
    private Integer sentCount;

    /**
     * Количество ошибок.
     */
    private Integer failedCount;

    /**
     * Время начала отправки.
     */
    private Instant startedAt;

    /**
     * Время завершения отправки.
     */
    private Instant finishedAt;

    /**
     * Проверка успешности отправки.
     * 
     * @return true если все уведомления отправлены
     */
    public boolean isSent() {
        return "SENT".equals(status);
    }

    /**
     * Проверка частичной отправки.
     * 
     * @return true если часть уведомлений отправлена
     */
    public boolean isPartial() {
        return "PARTIAL".equals(status);
    }

    /**
     * Проверка неудачи.
     * 
     * @return true если отправка не удалась
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}
