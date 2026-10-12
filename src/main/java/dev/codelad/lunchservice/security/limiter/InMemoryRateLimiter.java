package dev.codelad.lunchservice.security.limiter;

import dev.codelad.lunchservice.exception.RateLimitException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter для защиты от brute-force атак.
 * Используется для endpoint cancelByCode, который принимает произвольные коды отмены.
 *
 * Ограничение: 5 попыток на 1 минуту на один IP-адрес.
 * Работает в рамках одного JVM-процесса (для кластера нужен Redis-based limiter).
 */
@Component
public class InMemoryRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000L; // 1 минута

    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();

    /**
     * Проверить, не превышен ли лимит для IP.
     *
     * @param ip IP-адрес клиента
     * @throws RateLimitException если лимит превышен
     */
    public void checkLimit(String ip) {
        long now = System.currentTimeMillis();
        RateLimitEntry entry = attempts.compute(ip, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateLimitEntry(now, 1);
            }
            return new RateLimitEntry(existing.windowStart, existing.count + 1);
        });

        if (entry.count > MAX_ATTEMPTS) {
            throw new RateLimitException("Слишком много попыток. Попробуйте через минуту");
        }
    }

    private record RateLimitEntry(long windowStart, int count) {
    }
}
