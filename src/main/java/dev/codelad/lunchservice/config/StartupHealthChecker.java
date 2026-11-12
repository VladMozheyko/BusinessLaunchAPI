package dev.codelad.lunchservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Проверка доступности внешних сервисов при старте приложения.
 * Запускается перед инициализацией Spring Boot (order = -1).
 * <p>
 * Проверяет:
 * - Notification Service (через X-Internal-Service-Key)
 * - Food Service
 * - Venue Service
 */
@Component
@Order(-1)
@Slf4j
public class StartupHealthChecker implements CommandLineRunner {

    @Value("${notification.service.url:http://localhost:8082}")
    private String notificationServiceUrl;

    @Value("${food.service.url:http://localhost:8086}")
    private String foodServiceUrl;

    @Value("${venue.service.url:https://center.beer}")
    private String venueServiceUrl;

    @Value("${notification.internal.service.key:}")
    private String internalServiceKey;

    @Value("${food.service.path:/api/v2/food}")
    private String foodServicePath;

    @Value("${venue.service.path:/api/v1/venues}")
    private String venueServicePath;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(String... args) {
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║         STARTUP HEALTH CHECK — Checking Services        ║");
        log.info("╚══════════════════════════════════════════════════════════╝");

        boolean allOk = true;

        allOk &= checkNotificationService();
        allOk &= checkFoodService();
        allOk &= checkVenueService();

        if (allOk) {
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║              ✅ ALL SERVICES ARE REACHABLE              ║");
            log.info("╚══════════════════════════════════════════════════════════╝");

        } else {
            log.warn("╔══════════════════════════════════════════════════════════╗");
            log.warn("║  ⚠️  SOME SERVICES ARE UNREACHABLE — Check configuration  ║");
            log.warn("╚══════════════════════════════════════════════════════════╝");
        }
    }

    /**
     * Проверка Notification Service.
     * Использует /api/public/devices (POST) с заведомо невалидным payload.
     *
     * Ожидаемое поведение: 400 Bad Request с телом, содержащим "error" —
     * значит сервис жив, валидация работает, контракт подтверждён.
     *
     * HttpClientErrorException (4xx) — ожидаемый кейс.
     * RestClientException (connection refused, timeout, 5xx) — реальная проблема.
     */
    private boolean checkNotificationService() {
        log.info("");
        log.info("─".repeat(60));
        log.info("📬 Notification Service: {}", notificationServiceUrl);
        log.info("─".repeat(60));

        String url = notificationServiceUrl + "/api/public/devices";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalServiceKey != null && !internalServiceKey.isBlank()) {
            headers.set("X-Internal-Service-Key", internalServiceKey);
        }

        // Заведомо невалидный payload — валидатор обязан его отвергнуть.
        // Не дёргаем эндпоинт с побочными эффектами валидным запросом.
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            // Сюда попасть не должны: пустой запрос не может пройти валидацию
            log.warn("⚠️  [WARN]   Notification Service accepted empty payload (unexpected)");
            return false;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST
                    && e.getResponseBodyAsString().contains("error")) {
                log.info("✅ [OK]     Notification Service: контракт подтверждён (400 error)");
                return true;
            }
            // 400 без "error" — не ожидаемый кейс
            log.warn("⚠️  [WARN]   Notification Service returned 400 without 'error': {}", e.getResponseBodyAsString());
            return false;

        } catch (RestClientException e) {
            // connection refused, timeout, 5xx — сервис недоступен
            log.error("❌ [FAIL]   Cannot reach Notification Service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверка Food Service.
     * Использует /api/v1/food/menu/{venueId} (GET) — с произвольным ID.
     */
    private boolean checkFoodService() {
        log.info("");
        log.info("─".repeat(60));
        log.info("🍽️  Food Service: {}", foodServiceUrl);
        log.info("─".repeat(60));

        String url = foodServiceUrl + foodServicePath;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [OK]     Food Service is reachable (HTTP {})", response.getStatusCode().value());
            } else {
                log.warn("⚠️  [WARN]   Food Service returned HTTP {}", response.getStatusCode().value());
            }
            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException e) {
            log.error("❌ [FAIL]   Cannot reach Food Service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверка Venue Service.
     * Использует /api/v1/venues/{venueId} (GET) — с произвольным ID.
     */
    private boolean checkVenueService() {
        log.info("");
        log.info("─".repeat(60));
        log.info("🏢  Venue Service: {}", venueServiceUrl);
        log.info("─".repeat(60));

        String url = venueServiceUrl + venueServicePath;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [OK]     Venue Service is reachable (HTTP {})", response.getStatusCode().value());
            } else {
                log.warn("⚠️  [WARN]   Venue Service returned HTTP {}", response.getStatusCode().value());
            }
            return response.getStatusCode().is2xxSuccessful();

        } catch (RestClientException e) {
            log.error("❌ [FAIL]   Cannot reach Venue Service: {}", e.getMessage());
            return false;
        }
    }
}
