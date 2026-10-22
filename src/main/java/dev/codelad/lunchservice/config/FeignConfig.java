package dev.codelad.lunchservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Конфигурация Feign-клиентов для LunchService.
 * 
 * Содержит:
 * - RequestInterceptor для добавления заголовка X-Internal-Service-Key
 * - Все Feign-клиенты будут автоматически добавлять этот заголовок
 */
@Configuration
@Slf4j
public class FeignConfig {

    @Value("${notification.internal.service.key}")
    private String internalServiceKey;

    /**
     * Interceptor для всех Feign-клиентов.
     * Автоматически добавляет заголовок X-Internal-Service-Key к запросам.
     * 
     * Этот заголовок используется Notification Service для аутентификации
     * внутренних микросервисов (LunchService).
     */
    @Bean
    public RequestInterceptor internalServiceKeyInterceptor() {
        return template -> {
            // Добавляем API Key для всех запросов к Notification Service
            template.url();
            if (template.url().contains("/api/notifications/")) {
                template.header("X-Internal-Service-Key", internalServiceKey);
                log.debug("[FEIGN] Added X-Internal-Service-Key header to: {}", template.url());
            }
        };
    }
}
