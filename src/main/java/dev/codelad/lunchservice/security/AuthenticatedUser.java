package dev.codelad.lunchservice.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

/**
 * Аутентифицированный пользователь из JWT.
 * Содержит информацию из JWT и из БД (роль).
 */
@Data
@Builder
public class AuthenticatedUser {

    private final UUID userId;           // внутренний ID из БД этого сервиса
    private final UUID externalId;       // userUuid из JWT — для обращений к Auth-сервису
    private final String email;          // из JWT (может быть устаревшим)
    private final Role role;             // из БД этого сервиса
    private final Collection<GrantedAuthority> authorities;

    public boolean hasEmail() {
        return email != null && !email.isEmpty();
    }

    public boolean hasRole(Role role) {
        return authorities != null && authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(role.getAuthority()));
    }
}
