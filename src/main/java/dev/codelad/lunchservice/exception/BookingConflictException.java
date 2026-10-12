package dev.codelad.lunchservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение выбрасывается, когда нет мест для бронирования ланча.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }
}
