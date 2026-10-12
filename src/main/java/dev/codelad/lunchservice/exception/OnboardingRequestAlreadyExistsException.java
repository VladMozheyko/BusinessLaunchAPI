package dev.codelad.lunchservice.exception;

import java.util.UUID;

/**
 * Активная заявка уже существует.
 */
public class OnboardingRequestAlreadyExistsException extends RuntimeException {

    public OnboardingRequestAlreadyExistsException(UUID userId) {
        super("У пользователя " + userId + " уже есть активная заявка");
    }
}
