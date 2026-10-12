package dev.codelad.lunchservice.util;

import java.security.SecureRandom;

/**
 * Утилита для генерации уникальных кодов отмены бронирований.
 * Формат: XXX-XXX-XXX (3 блока, каждый: 1 буква A-Z + 2 цифры 0-9).
 */
public final class CancelCodeGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final int NUM_BLOCKS = 3;
    private static final String SEPARATOR = "-";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CancelCodeGenerator() {
        // Утилита, экземпляр не нужен
    }

    /**
     * Сгенерировать уникальный код отмены в формате XXX-XXX-XXX.
     *
     * @return сгенерированный код отмены
     */
    public static String generate() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < NUM_BLOCKS; i++) {
            if (i > 0) {
                code.append(SEPARATOR);
            }
            code.append(LETTERS.charAt(SECURE_RANDOM.nextInt(LETTERS.length())));
            code.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
            code.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        }

        return code.toString();
    }
}
