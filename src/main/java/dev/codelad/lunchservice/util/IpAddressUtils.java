package dev.codelad.lunchservice.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Утилита для работы с HTTP запросами.
 */
public final class IpAddressUtils {

    private IpAddressUtils() {
        // Утилита, экземпляр не нужен
    }

    /**
     * Извлечь IP-адрес клиента из запроса (учитывает proxy).
     *
     * @param request HTTP запрос
     * @return IP-адрес клиента
     */
    public static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
