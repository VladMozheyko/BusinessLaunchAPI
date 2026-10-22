package dev.codelad.lunchservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Утилита для получения информации о текущем пользователе из SecurityContext.
 */
@Service
@RequiredArgsConstructor
public class SecurityContextHelper {

    /**
     * Получить текущего пользователя (lenient — возвращает null если не аутентифицирован).
     */
    public AuthenticatedUser getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    /**
     * Получить внутренний ID пользователя (null для Bar Auth).
     */
    public UUID getCurrentUserId() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * Получить externalId пользователя (userUuid из Base Auth или user_id из Bar Auth).
     * Возвращается как String для поддержки UUID (550e8400-...) и числовых ID (1001, 2001).
     */
    public String getCurrentExternalId() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getExternalId() : null;
    }

    /**
     * Получить email текущего пользователя.
     */
    public String getCurrentUserEmail() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Получить роль текущего пользователя.
     */
    public Role getCurrentUserRole() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * Проверить, есть ли у текущего пользователя указанная роль.
     */
    public boolean hasRole(Role role) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null) return false;
        return user.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), role.getAuthority()));
    }

    /**
     * Проверить, является ли пользователь глобальным администратором.
     */
    public boolean isAdmin() {
        return hasRole(Role.ROLE_GLOBAL_ADMIN);
    }

    /**
     * Проверить, является ли пользователь владельцем бара.
     */
    public boolean isBarOwner() {
        return hasRole(Role.ROLE_BAR_OWNER);
    }

    /**
     * Проверить, является ли пользователь владельцем пивоварни.
     */
    public boolean isBrewOwner() {
        return hasRole(Role.ROLE_BREW_OWNER);
    }

    /**
     * Проверить, является ли пользователь владельцем (бар или пивоварня).
     */
    public boolean isOwner() {
        return isBarOwner() || isBrewOwner();
    }

    /**
     * Получить текущего пользователя (strict — выбрасывает исключение если не аутентифицирован).
     */
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Пользователь не аутентифицирован");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        throw new IllegalStateException("Не удалось получить текущего пользователя");
    }
}
