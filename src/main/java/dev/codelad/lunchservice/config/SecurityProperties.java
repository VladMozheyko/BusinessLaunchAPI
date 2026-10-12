package dev.codelad.lunchservice.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация безопасности: JWT secret и заголовок токена.
 * Secret key должен быть установлен через application.yml или env-переменные.
 */
@Getter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityProperties {

    private String secretKey;

    private String header = "Authorization";

    private String prefix = "Bearer ";

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        log.debug("[SecurityProperties] Секретный ключ JWT настроен (длина: {})",
                secretKey != null ? secretKey.length() : 0);
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
