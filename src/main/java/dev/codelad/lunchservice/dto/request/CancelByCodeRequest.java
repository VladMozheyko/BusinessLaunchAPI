package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на отмену бронирования по коду.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на отмену бронирования по коду")
public class CancelByCodeRequest {

    @NotBlank(message = "cancelCode обязателен")
    @Pattern(
            regexp = "[A-Z0-9]{3}-[A-Z0-9]{3}-[A-Z0-9]{3}",
            message = "cancelCode должен быть в формате XXX-XXX-XXX (3 блока по 3 символа через тире)"
    )
    @Schema(
            description = "Уникальный код отмены бронирования",
            example = "A1B-2C3-D4E",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String cancelCode;
}
