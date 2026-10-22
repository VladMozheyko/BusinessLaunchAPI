package dev.codelad.lunchservice.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO для передачи информации о пользователе между слоями.
 * Создаётся JwtAuthenticationFilter после валидации JWT.
 */
@Data
@Builder
public class AuthenticatedUser {

    private final UUID userId;           // внутренний ID из БД этого сервиса (null для Bar Auth)
    private final String externalId;     // userUuid (UUID как String) или user_id (1001, 2001) из Bar Auth
    private final String email;          // из JWT (может отличаться от email в БД)
    private final Role role;             // из БД (Auth-сервис) или из JWT claims (Bar Auth)
    private final Collection<GrantedAuthority> authorities;

    private final Long authUserId;       // user_id из Bar Auth JWT
    private final String userType;       // "bar", "brew", "admin"
    private final Long venueId;          // bar_id / brew_id из Bar Auth JWT

    public boolean hasEmail() {
        return email != null && !email.isEmpty();
    }

    public boolean hasRole(Role role) {
        return authorities != null && authorities.stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), role.getAuthority()));
    }

    // Методы для Bar Auth
    public boolean isBarOwner() {
        return userType != null && "bar".equals(userType);
    }

    public boolean isBrewOwner() {
        return userType != null && "brew".equals(userType);
    }

    public boolean isOwner() {
        return isBarOwner() || isBrewOwner();
    }

    public boolean isAdmin() {
        return userType != null && "admin".equals(userType);
    }
}
