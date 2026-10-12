package dev.codelad.lunchservice.config;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;

/**
 * Фильтр для инициализации контекста текущего HTTP-запроса.
 * <p>
 * Извлекает путь запроса (requestURI) и сохраняет его в {@link RequestContext},
 * который доступен через request-scope bean на протяжении всего жизненного цикла запроса.
 * <p>
 * Это позволяет получать путь запроса из любого места приложения
 * без необходимости явно передавать {@code HttpServletRequest} в каждый метод.
 * <p>
 * <b>Пример использования:</b>
 * <pre>
 * {@code @Autowired}
 * private RequestContext requestContext;
 *
 * log.info("[ACTION] Выполнено действие на: {}", requestContext.getPath());
 * </pre>
 * <p>
 * <b>Важно:</b> Фильтр выполняется один раз на каждый HTTP-запрос (благодаря {@code OncePerRequestFilter}).
 * Если {@code request} равен {@code null}, инициализация пропускается.
 *
 * @see RequestContext
 * @see OncePerRequestFilter
 */
@Component("lunchRequestContextFilter")
@RequiredArgsConstructor
public class RequestContextFilter extends OncePerRequestFilter {

    /**
     * Поставщик (ObjectProvider) для получения request-scope бина RequestContext.
     * ObjectProvider используется для ленивой загрузки и проверки на null.
     */
    private final ObjectProvider<RequestContext> requestContextProvider;

    /**
     * Извлекает путь текущего HTTP-запроса и сохраняет его в RequestContext.
     * <p>
     * Выполняется один раз на каждый запрос. Если request или requestContext равны null,
     * инициализация пропускается без ошибок.
     *
     * @param request  HTTP-запрос (может быть null)
     * @param response HTTP-ответ (может быть null)
     * @param filterChain цепочка фильтров для продолжения обработки
     * @throws ServletException если произошла ошибка сервлета
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(
            @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable FilterChain filterChain)
            throws ServletException, IOException {
        RequestContext requestContext = requestContextProvider.getObject();
        if (request != null) {
            requestContext.setPath(request.getRequestURI());
        }
        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }
}
