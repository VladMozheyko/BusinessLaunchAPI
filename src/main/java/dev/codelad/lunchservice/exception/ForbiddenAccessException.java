package dev.codelad.lunchservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение для 403 Forbidden.
 * Выбрасывается, когда пользователь аутентифицирован, но не имеет прав.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException() {
        super("У вас нет прав для выполнения этого действия");
    }

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
