package dev.codelad.lunchservice.config;

/**
 * Утилита для работы с профилем Spring.
 * <p>
 * Предоставляет методы для получения активного профиля приложения
 * из системных свойств или переменных окружения.
 * <p>
 * <b>Порядок определения профиля:</b>
 * <ol>
 *   <li>Системное свойство {@code spring.profiles.active}</li>
 *   <li>Переменная окружения {@code SPRING_PROFILES_ACTIVE}</li>
 *   <li>По умолчанию — {@code "default"}</li>
 * </ol>
 * <p>
 * <b>Пример использования:</b>
 * <pre>
 * if (ProfileUtil.isActive("dev")) {
 *     // Dev-специфичная логика
 * }
 * </pre>
 *
 * @see <a href="https://docs.spring.io/spring-boot/how-to/properties-and-configuration.html">Spring Boot Properties</a>
 */
public final class ProfileUtil {

    private static final String SYSTEM_PROPERTY_KEY = "spring.profiles.active";
    private static final String ENV_VAR_KEY = "SPRING_PROFILES_ACTIVE";
    private static final String DEFAULT_PROFILE = "default";

    /**
     * Приватный конструктор для предотвращения инстанцирования утилиты.
     */
    private ProfileUtil() {
        throw new UnsupportedOperationException("Утилита не может быть инстанцирована");
    }

    /**
     * Получить активный профиль Spring.
     * <p>
     * Проверяет системные свойства, затем переменные окружения.
     * Если ничего не найдено — возвращает {@code "default"}.
     *
     * @return имя активного профиля, не null
     */
    public static String getActiveProfile() {
        String profile = System.getProperty(SYSTEM_PROPERTY_KEY);
        if (profile != null && !profile.isEmpty()) {
            return profile.trim();
        }

        String envProfile = System.getenv(ENV_VAR_KEY);
        if (envProfile != null && !envProfile.isEmpty()) {
            return envProfile.trim();
        }

        return DEFAULT_PROFILE;
    }

    /**
     * Проверить, активен ли указанный профиль.
     * <p>
     * Удобно для условной логики:
     * <pre>
     * if (ProfileUtil.isActive("dev")) { ... }
     * </pre>
     *
     * @param profileName имя профиля для проверки (не null, не пустой)
     * @return {@code true}, если профиль активен
     * @throws IllegalArgumentException если profileName null или пустой
     */
    public static boolean isActive(String profileName) {
        if (profileName == null || profileName.isEmpty()) {
            throw new IllegalArgumentException("Имя профиля не может быть null или пустым");
        }
        return getActiveProfile().equals(profileName.trim());
    }
}
