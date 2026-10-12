package dev.codelad.lunchservice.config;

import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

/**
 * Контекст текущего HTTP-запроса.
 * <p>
 * Хранит информацию о текущем запросе в рамках request-скупа Spring.
 * Позволяет получать путь запроса (path) из любого места приложения
 * без необходимости передавать HttpServletRequest в каждый метод.
 * <p>
 * Используется для:
 * <ul>
 *   <li>Логирования действий пользователей с указанием endpoint</li>
 *   <li>Аудита и отслеживания активности</li>
 *   <li>Формирования сообщений об ошибках с указанием пути</li>
 * </ul>
 * <p>
 * Пример использования:
 * <pre>
 * {@code @Autowired}
 * private RequestContext requestContext;
 *
 * log.info("[ACTION] Пользователь выполнил действие на: {}", requestContext.getPath());
 * </pre>
 * <p>
 * <b>Важно:</b> Компонент доступен только в рамках одного HTTP-запроса.
 * При использовании в синхронизированных потоках может потребоваться
 * дополнительная синхронизация контекста.
 *
 * @see org.springframework.web.context.request.RequestContextHolder
 */
@Setter
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {

    /**
     * Путь запроса (например, "/api/v1/lunch/combo").
     * Устанавливается фильтром или контроллером в начале обработки запроса.
     */
    private String path;

    /**
     * Получить путь текущего запроса.
     * <p>
     * Если path не установлен (например, запрос не прошёл через инициализацию),
     * возвращает "unknown".
     *
     * @return путь запроса или "unknown", если путь не определён
     */
    public String getPath() {
        return path != null ? path : "unknown";
    }

}
