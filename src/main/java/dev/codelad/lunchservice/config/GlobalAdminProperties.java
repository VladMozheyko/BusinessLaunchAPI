package dev.codelad.lunchservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Свойства для дефолтного глобального администратора.
 * Email берётся из внешнего сервиса аутентификации (JWT claim "email").
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.admin.bootstrap-email")
public class GlobalAdminProperties {

    /**
     * Email глобального админа.
     * Используется только при первом входе пользователя (если не найден в БД по externalId).
     * Работает один раз за всю жизнь системы — после создания первого GLOBAL_ADMIN
     */
    private String defaultEmail;
}
