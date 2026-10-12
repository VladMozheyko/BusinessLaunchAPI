package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на отмену бронирования по коду и номеру телефона.
 * Доступно для незарегистрированных пользователей.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на отмену бронирования по коду и номеру телефона")
public class CancelByPhoneRequest {

    @NotBlank(message = "cancelCode обязателен")
    @Pattern(regexp = "^[A-Z0-9]{3}-[A-Z0-9]{3}-[A-Z0-9]{3}$", message = "cancelCode должен быть в формате XXX-XXX-XXX")
    @Schema(
            description = "Код отмены бронирования",
            example = "A1B-2C3-D4E",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String cancelCode;

    @NotBlank(message = "phoneNumber обязателен")
    @Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "phoneNumber должен содержать от 7 до 20 символов")
    @Schema(
            description = "Номер телефона пользователя",
            example = "+79991234567",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String phoneNumber;
}
