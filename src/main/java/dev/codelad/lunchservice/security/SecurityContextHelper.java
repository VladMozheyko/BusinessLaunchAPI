package dev.codelad.lunchservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Утилита для получения информации о текущем аутентифицированном пользователе.
 * Используется в контроллерах и сервисах вместо прямого обращения к SecurityContext.
 * <p>
 * <b>Возможности:</b>
 * <ul>
 *   <li>Получение AuthenticatedUser из SecurityContext</li>
 *   <li>Получение ID пользователя (внутренний и externalId)</li>
 *   <li>Получение email и роли текущего пользователя</li>
 *   <li>Проверка ролей: isAdmin(), isFranchiseOwner(), isLocationAdmin()</li>
 * </ul>
 * <p>
 * <b>Пример использования:</b>
 * <pre>
 * {@code @Autowired}
 * private SecurityContextHelper securityContextHelper;
 *
 * // Получить email
 * String email = securityContextHelper.getCurrentUserEmail();
 *
 * // Проверить роль
 * if (securityContextHelper.isFranchiseOwner()) {
 *     // Логика для франчайзи
 * }
 * </pre>
 *
 * @see AuthenticatedUser
 * @see Role
 */
@Service
@RequiredArgsConstructor
public class SecurityContextHelper {

    /**
     * Получить текущего аутентифицированного пользователя.
     *
     * @return AuthenticatedUser, если пользователь аутентифицирован, иначе null
     */
    public AuthenticatedUser getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    /**
     * Получить внутренний ID пользователя из БД.
     *
     * @return UUID пользователя, если аутентифицирован, иначе null
     */
    public UUID getCurrentUserId() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * Получить externalId (userUuid из JWT).
     *
     * @return externalId пользователя, если аутентифицирован, иначе null
     */
    public UUID getCurrentExternalId() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getExternalId() : null;
    }

    /**
     * Получить email текущего пользователя.
     *
     * @return email пользователя, если аутентифицирован, иначе null
     */
    public String getCurrentUserEmail() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Получить роль текущего пользователя из БД.
     *
     * @return Role пользователя, если аутентифицирован, иначе null
     */
    public Role getCurrentUserRole() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * Проверить, имеет ли пользователь указанную роль.
     *
     * @param role роль для проверки
     * @return true, если пользователь имеет указанную роль
     */
    public boolean hasRole(Role role) {
        AuthenticatedUser user = getAuthenticatedUser();
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), role.getAuthority()));
    }

    /**
     * Проверить, является ли пользователь глобальным админом.
     *
     * @return true, если роль GLOBAL_ADMIN
     */
    public boolean isAdmin() {
        return hasRole(Role.ROLE_GLOBAL_ADMIN);
    }

    /**
     * Проверить, является ли пользователь владельцем франшизы.
     *
     * @return true, если роль FRANCHISE_OWNER
     */
    public boolean isFranchiseOwner() {
        return hasRole(Role.ROLE_FRANCHISE_OWNER);
    }

    /**
     * Проверить, является ли пользователь администратором локации.
     *
     * @return true, если роль LOCATION_ADMIN
     */
    public boolean isLocationAdmin() {
        return hasRole(Role.ROLE_LOCATION_ADMIN);
    }

    /**
     * Получить текущего аутентифицированного пользователя.
     * Альтернативное название для getAuthenticatedUser().
     *
     * @return AuthenticatedUser, если пользователь аутентифицирован, иначе null
     * @throws IllegalStateException если пользователь не аутентифицирован
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

    /**
     * Получить ID текущего пользователя (externalId из Auth-сервиса).
     *
     * @return externalId пользователя
     * @throws IllegalStateException если пользователь не аутентифицирован
     */
    public UUID getCurrentUserExternalIdStrict() {
        return getCurrentUser().getUserId();
    }
}
