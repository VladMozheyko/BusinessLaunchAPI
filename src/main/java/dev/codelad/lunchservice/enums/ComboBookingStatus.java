package dev.codelad.lunchservice.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * <p>
 * Статус бронирования комбо-наборов.
 * </p>
 * Workflow:
 * 1. PENDING - на подтверждении
 * 2. APPROVED - одобрен/выполняется
 * 3. COMPLETED - выполнен (финальный статус)
 * 4. CANCELLED - отменён
 */
public enum ComboBookingStatus {

    /** На подтверждении */
    PENDING("PENDING"),

    /** Одобрен/выполняется */
    APPROVED("APPROVED"),

    /** Выполнен (финальный) */
    COMPLETED("COMPLETED"),

    /** Отменён */
    CANCELLED("CANCELLED");

    private final String value;

    ComboBookingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
