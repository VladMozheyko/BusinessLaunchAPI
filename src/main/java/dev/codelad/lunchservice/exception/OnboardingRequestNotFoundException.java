package dev.codelad.lunchservice.exception;

import java.util.UUID;

/**
 * Заявка не найдена.
 */
public class OnboardingRequestNotFoundException extends RuntimeException {

    public OnboardingRequestNotFoundException(Long id) {
        super("Заявка с ID " + id + " не найдена");
    }
}
