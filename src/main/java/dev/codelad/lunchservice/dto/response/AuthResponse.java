package dev.codelad.lunchservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Ответ с JWT-токеном для пользователя.
 * Токен действует 15 минут.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ с JWT-токеном после успешной аутентификации")
public class AuthResponse {

    @Schema(
            description = "JWT-токен для доступа к API (действует 15 минут)",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDQiLCJ1c2VyVXVpZCI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwNCIsImVtYWlsIjoidXNlckBjb21wYW55LmNvbSIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDAxMDAwfQ.abc123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String token;

    @Schema(
            description = "UUID пользователя во внешней системе",
            example = "550e8400-e29b-41d4-a716-446655440004"
    )
    private UUID userId;

    @Schema(
            description = "Email пользователя",
            example = "user@company.com"
    )
    private String email;

    @Schema(
            description = "Роль пользователя в системе",
            example = "ROLE_USER"
    )
    private String role;

    @Schema(
            description = "Время истечения токена (UTC, через 15 минут)",
            example = "2024-01-15T14:15:00Z"
    )
    private Instant expiresIn;
}
