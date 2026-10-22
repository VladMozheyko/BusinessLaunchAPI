package dev.codelad.lunchservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение для 401 Unauthorized.
 * Выбрасывается, когда пользователь не аутентифицирован.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("Пользователь не аутентифицирован");
    }

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
