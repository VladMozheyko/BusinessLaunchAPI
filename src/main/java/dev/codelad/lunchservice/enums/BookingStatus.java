package dev.codelad.lunchservice.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * <p>
 * Статус бронирования бизнес-ланча.
 * </p>
 * Workflow:
 * 1. PENDING - на подтверждении (пользователь может отменить в любой момент)
 * 2. APPROVED - одобрен/выполняется (пользователь может отменить, админ может отменить)
 * 3. COMPLETED - выполнен (финальный статус)
 * 4. CANCELLED - отменён (отмена пользователем или админом)
 */
public enum BookingStatus {

    /**
     * На подтверждении - заказ создан, ожидает подтверждения админом.
     */
    PENDING("PENDING"),

    /**
     * Одобрен - заказ принят в работу/выполняется.
     */
    APPROVED("APPROVED"),

    /**
     * Выполнен - заказ выполнен (финальный статус).
     */
    COMPLETED("COMPLETED"),

    /**
     * Отменён - отменён пользователем или админом.
     */
    CANCELLED("CANCELLED");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
