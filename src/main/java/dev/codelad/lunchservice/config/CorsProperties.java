package dev.codelad.lunchservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация CORS (Cross-Origin Resource Sharing).
 * <p>
 * Позволяет настраивать разрешённые источники (origins) для каждого профиля
 * без изменения кода. IP-адреса и домены вынесены в {@code application-*.properties}.
 * <p>
 * <b>Пример конфигурации в properties:</b>
 * <pre>
 * # application-local.properties
 * app.cors.allowed-origins=<a href="http://111.88.158.230:8486,http://localhost:8080">...</a>
 *
 * # application-prod.properties
 * app.cors.allowed-origins=https://canteen.example.com,https://api.example.com
 * </pre>
 * <p>
 * <b>Безопасность:</b>
 * <ul>
 *   <li>В production обязательно указывайте конкретные домены, а не {@code *}</li>
 *   <li>Избегайте хардкода IP-адресов — используйте переменные окружения или конфигурационные файлы</li>
 *   <li>Регулярно проверяйте актуальность разрешённых origins при изменении инфраструктуры</li>
 * </ul>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">CORS на MDN</a>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Список разрешённых источников (origins).
     * <p>
     * Формат: полный URL с протоколом, например:
     * <ul>
     *   <li>{@code http://localhost:3000}</li>
     *   <li>{@code https://example.com}</li>
     *   <li>{@code http://192.168.1.100:8080}</li>
     * </ul>
     * <p>
     * <b>Важно:</b>
     * <ul>
     *   <li>Не используйте {@code *} в production</li>
     *   <li>Указывайте протокол (http:// или https://)</li>
     *   <li>Порядок не имеет значения</li>
     * </ul>
     */
    private List<String> allowedOrigins;
}
