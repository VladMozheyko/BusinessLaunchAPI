package dev.codelad.lunchservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение выбрасывается при превышении лимита запросов (rate limit).
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
