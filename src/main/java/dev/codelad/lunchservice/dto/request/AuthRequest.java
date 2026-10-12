package dev.codelad.lunchservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на аутентификацию и получение JWT-токена.
 * Для тестовых пользователей достаточно указать email.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на аутентификацию для получения JWT-токена")
public class AuthRequest {

    @NotBlank(message = "email обязателен")
    @Email(message = "email должен быть валидным")
    @Schema(
            description = """
                    Email тестового пользователя. Доступные email:
                      - admin@company.com (ROLE_GLOBAL_ADMIN)
                      - franchise@company.com (ROLE_FRANCHISE_OWNER)
                      - location@company.com (ROLE_LOCATION_ADMIN)
                      - user@company.com (ROLE_USER)
                      - guest@company.com (ROLE_GUEST)
                    """,
            example = "user@company.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;
}
