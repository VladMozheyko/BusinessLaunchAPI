package dev.codelad.lunchservice.constant;

/**
 * Константы для строковых литералов, используемых в нескольких местах.
 * Позволяет легко рефакторить и изменять сообщения в одном месте.
 */
public final class Messages {

    // ==================== Booking Status Messages ====================
    public static final String BOOKING_NOT_FOUND = "Бронирование с ID %s не найдено";
    public static final String BOOKING_CANCELLED = "Бронирование %s уже отменено";
    public static final String BOOKING_COMPLETED = "Нельзя отменить выполненный заказ %s";
    public static final String CANCEL_BOOKING_NOT_FOUND = "Бронирование с кодом отмены '%s' не найдено";
    public static final String CANCEL_BOOKING_NOT_FOUND_OR_PHONE_MISMATCH =
            "Бронирование с кодом '%s' не найдено или номер телефона не совпадает";
    public static final String APPROVE_PENDING_ONLY =
            "Можно подтвердить только бронирование в статусе PENDING. Текущий статус: %s";
    public static final String COMPLETE_APPROVED_ONLY =
            "Можно выполнить только бронирование в статусе APPROVED. Текущий статус: %s";
    public static final String ACCESS_DENIED = "У вас нет доступа к этому бронированию";

    // ==================== Combo Booking Messages ====================
    public static final String COMBO_NOT_FOUND = "Комбо с ID %s не найдено";
    public static final String COMBO_NOT_BELONGS_TO_VENUE =
            "Комбо %s не принадлежит заведению %s";
    public static final String COMBO_UNAVAILABLE = "Комбо \"%s\" сейчас недоступно";
    public static final String COMBO_DAILY_LIMIT_EXCEEDED =
            "Превышен дневной лимит комбо \"%s\": доступно ещё %d из %d";
    public static final String ADDON_NOT_VALID =
            "Блюдо \"%s\" не является дополнением к комбо \"%s\"";
    public static final String COMBO_BOOKING_BY_PHONE =
            "Комбо-бронирования для телефона %s: %d шт.";

    // ==================== Venue Messages ====================
    public static final String VENUE_NOT_FOUND = "Заведение с ID %s не найдено";
    public static final String VENUE_NOT_WORKING_DAY =
            "Заведение %s не работает в этот день недели";
    public static final String VENUE_NOT_WORKING = "Заведение не работает в этот день";
    public static final String VENUE_OUTSIDE_HOURS =
            "Заведение работает с %s до %s. Выбранное время %s вне этого интервала";
    public static final String VENUE_NO_CAPACITY =
            "Нет мест: доступно ещё %d из %d";

    // ==================== Category Messages ====================
    public static final String CATEGORY_NOT_FOUND = "Категория с ID %s не найдена";
    public static final String CATEGORY_NOT_BELONGS_TO_VENUE =
            "Категория %s не принадлежит заведению %s";
    public static final String CATEGORY_UNAVAILABLE =
            "Категория с ID %s не доступна для заказа";

    // ==================== Dish Messages ====================
    public static final String DISH_NOT_FOUND = "Блюдо с ID %s не найдено";
    public static final String DISH_NOT_BELONGS_TO_CATEGORY =
            "Блюдо %s не принадлежит категории %s";
    public static final String DISH_UNAVAILABLE = "Блюдо \"%s\" сейчас недоступно";
    public static final String DISH_QUANTITY_LIMIT =
            "Доступно только %d порций блюда \"%s\"";

    // ==================== General Messages ====================
    public static final String TOTAL_GUESTS_MUST_BE_POSITIVE =
            "Общее количество гостей должно быть больше 0";
    public static final String QUANTITY_MUST_BE_POSITIVE =
            "Количество должно быть больше 0";

    // ==================== Auth Messages ====================
    public static final String INVALID_TEST_EMAIL =
            "Недопустимый email для тестовой аутентификации: %s";

    private Messages() {
        // Утилита, экземпляр не нужен
    }
}
