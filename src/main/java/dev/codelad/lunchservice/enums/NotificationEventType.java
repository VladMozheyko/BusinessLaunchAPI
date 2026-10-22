package dev.codelad.lunchservice.enums;

/**
 * Типы событий, которые могут вызывать уведомления.
 * Используются для определения шаблонов сообщений и логики отправки.
 */
public enum NotificationEventType {

    /**
     * Пользователь создал бронь обеда
     */
    BOOKING_CREATED("Бронь создана", "Ваша бронь в {venue} создана"),

    /**
     * Пользователь отменил бронь (по коду или по телефону)
     */
    BOOKING_CANCELLED("Бронь отменена", "Ваша бронь в {venue} отменена"),

    /**
     * Администратор отменил бронь
     */
    BOOKING_CANCELLED_BY_ADMIN("Бронь отменена админом", "Ваша бронь в {venue} отменена администратором"),

    /**
     * Администратор одобрил бронь
     */
    BOOKING_APPROVED("Бронь одобрена", "Ваша бронь в {venue} одобрена"),

    /**
     * Бронь завершена (пользователь посетил заведение)
     */
    BOOKING_COMPLETED("Бронь завершена", "Ваша бронь в {venue} завершена. Спасибо, что пришли!"),

    /**
     * Пользователь создал бронь комбо-набора
     */
    COMBO_BOOKING_CREATED("Комбо-бронь создана", "Ваша бронь комбо-набора в {venue} создана"),

    /**
     * Пользователь отменил бронь комбо-набора
     */
    COMBO_BOOKING_CANCELLED("Комбо-бронь отменена", "Ваша бронь комбо-набора в {venue} отменена"),

    /**
     * Администратор отменил бронь комбо-набора
     */
    COMBO_BOOKING_CANCELLED_BY_ADMIN("Комбо-бронь отменена админом", "Ваша бронь комбо-набора в {venue} отменена администратором"),

    /**
     * Администратор одобрил бронь комбо-набора
     */
    COMBO_BOOKING_APPROVED("Комбо-бронь одобрена", "Ваша бронь комбо-набора в {venue} одобрена"),

    /**
     * Бронь комбо-набора завершена
     */
    COMBO_BOOKING_COMPLETED("Комбо-бронь завершена", "Ваша бронь комбо-набора в {venue} завершена. Спасибо, что пришли!");

    /**
     * Краткое название события (для логов)
     */
    private final String shortName;

    /**
     * Шаблон заголовка уведомления (будет заполнен данными)
     */
    private final String titleTemplate;

    NotificationEventType(String shortName, String titleTemplate) {
        this.shortName = shortName;
        this.titleTemplate = titleTemplate;
    }

    public String getShortName() {
        return shortName;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }
}
