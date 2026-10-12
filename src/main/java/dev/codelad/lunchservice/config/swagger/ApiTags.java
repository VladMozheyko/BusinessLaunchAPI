/**
 * Suppress для S2068 — константы с AUTH/TOKEN в имени содержат только описания API, не секреты.
 */
package dev.codelad.lunchservice.config.swagger;

/**
 * Константы тегов для Swagger/OpenAPI документации.
 * Теги разделены на две группы:
 * 1. Доменные теги — группировка по бизнес-доменам (Категории, Блюда, Комбо, Бронирование)
 * 2. Ролевые теги — группировка по ролям (GL, FO, LA, USER)
 * Б
 * Каждый endpoint имеет оба тега: доменный и ролевой.
 * Это позволяет в Swagger UI фильтровать endpoints по доменам И по ролям одновременно.
 */
public final class ApiTags {

    private ApiTags() {}

    // ==================== ИНФОРМАЦИОННЫЕ ТЕГИ ====================

    /** Информация о ролях — подробности о ролях и правах доступа в системе */
    public static final String PRESENTATION = "📊 Информация о ролях";

    // ==================== ДОМЕННЫЕ ТЕГИ ====================

    /** Категории — управление категориями блюд */
    public static final String CATEGORIES = "📁 Категории";

    /** Блюда — управление блюдами в категориях */
    public static final String DISHES = "📁 Блюда";

    /** Комбо — управление комбо-наборами */
    public static final String COMBOS = "📁 Комбо";

    /** Бронирование ланчей — создание и управление бронированиями бизнес-ланчей */
    public static final String LUNCH_BOOKINGS = "📁 Бронирование ланчей";

    /** Онбординг — управление заявками на присоединение к системе */
    public static final String ONBOARDING = "📋 Онбординг";

    /** Карточки заведений — управление карточками бизнес-ланчей */
    public static final String VENUE_PROFILES = "📁 Карточки заведений";

    /** Аутентификация — получение JWT-токенов (только Dev/Local) */
    public static final String AUTH = "🔐 Аутентификация";

    // ==================== РОЛЕВЫЕ ТЕГИ ====================

    /** Глобальный администратор (GL) — полный доступ ко всем ресурсам */
    public static final String GLOBAL_ADMIN = "🛡️ Глобальный админ (GL)";

    /** Владелец франшизы (FO) — управление своими локациями */
    public static final String FRANCHISE_OWNER = "🛡️ Владелец франшизы (FO)";

    /** Локальный администратор (LA) — управление конкретной локацией */
    public static final String LOCATION_ADMIN = "🛡️ Локальный админ (LA)";

    /** Пользователь (USER) — авторизованный пользователь */
    public static final String USER = "🛡️ Пользователь (USER)";

    // ==================== ОПИСАНИЯ ТЕГОВ ====================

    public static final String PRESENTATION_DESC = "Подробное описание ролей и прав доступа в системе LunchService";

    public static final String CATEGORIES_DESC = "API для управления категориями блюд (создание, изменение isAddon, удаление)";
    public static final String DISHES_DESC = "API для управления блюдами (создание, обновление, изменение доступности, удаление)";
    public static final String COMBOS_DESC = "API для управления комбо-наборами (создание, изменение активности)";
    public static final String LUNCH_BOOKINGS_DESC = "API для бронирования бизнес-ланчей (создание бронирования)";
    public static final String ONBOARDING_DESC = "API для управления заявками на присоединение к системе (создание, рассмотрение, одобрение, отклонение, назначение LOCATION_ADMIN)";
    public static final String VENUE_PROFILES_DESC = "API для управления карточками заведений (создание, обновление, расписание, логотип)";
    /** Suppress: это описание API, а не реальный секрет. */
    @SuppressWarnings("squid:S2068")
    public static final String AUTH_DESC = "API для аутентификации и получения JWT-токенов (ТОЛЬКО для Dev/Local профилей)";

    public static final String GLOBAL_ADMIN_DESC = "Все методы для глобального администратора (полный доступ ко всему)";
    public static final String FRANCHISE_OWNER_DESC = "Все методы для владельца франшизы (управление своими локациями)";
    public static final String LOCATION_ADMIN_DESC = "Все методы для локального администратора (управление локацией)";
    public static final String USER_DESC = "Все методы для авторизованного пользователя";
}
