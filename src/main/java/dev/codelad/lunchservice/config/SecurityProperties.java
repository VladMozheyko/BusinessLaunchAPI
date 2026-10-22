package dev.codelad.lunchservice.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация безопасности: JWT secret и заголовок токена.
 * Secret key должен быть установлен через application.yml или env-переменные.
 * 
 * Поддерживает два источника аутентификации:
 * 1. Auth-сервис (secretKey) — JWT с userUuid, email. Роль берётся из БД.
 * 2. Bar Auth-сервис (barAuthSecretKey) — JWT с user_id, user_type, bar_id/brew_id.
 *    user_type определяет роль: "admin" → GLOBAL_ADMIN, "bar" → BAR_OWNER, "brew" → BREW_OWNER.
 */
@Getter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityProperties {

    /**
     * Secret key для валидации JWT от Auth-сервиса.
     * Auth-сервис выдаёт JWT с userUuid, email. Роль определяется в LunchService по userId из БД.
     */
    private String secretKey;

    /**
     * Secret key для валидации JWT от Bar Auth-сервиса.
     * Bar Auth-сервис выдаёт JWT с user_id, user_type, bar_id/brew_id.
     * user_type определяет роль: "admin" → GLOBAL_ADMIN, "bar" → BAR_OWNER, "brew" → BREW_OWNER.
     */
    private String barAuthSecretKey;

    private String header = "Authorization";

    private String prefix = "Bearer ";

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        log.debug("[SecurityProperties] Секретный ключ JWT настроен (длина: {})",
                secretKey != null ? secretKey.length() : 0);
    }

    public void setBarAuthSecretKey(String barAuthSecretKey) {
        this.barAuthSecretKey = barAuthSecretKey;
        log.debug("[SecurityProperties] Секретный ключ Bar Auth JWT настроен (длина: {})",
                barAuthSecretKey != null ? barAuthSecretKey.length() : 0);
    }

    public void setHeader(String header) {
        this.header = header;
        log.debug("[SecurityProperties] Заголовок JWT установлен: {}", header);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        log.debug("[SecurityProperties] Префикс JWT установлен: {}", prefix);
    }
}
