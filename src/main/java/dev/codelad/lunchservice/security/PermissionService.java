package dev.codelad.lunchservice.security;

import dev.codelad.lunchservice.exception.ForbiddenAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Сервис для проверки прав доступа.
 * Заменяет @PreAuthorize — вся логика авторизации здесь.
 * 
 * Примеры использования:
 * - checkOwnerAccess(authenticatedUser) — проверка BAR_OWNER/BREW_OWNER
 * - checkGlobalAdminAccess(authenticatedUser) — проверка GLOBAL_ADMIN
 * - checkVenueAccess(authenticatedUser, venueId) — проверка доступа к заведению
 * - checkManagerAccess(authenticatedUser, venueId) — проверка что пользователь менеджер заведения
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SecurityContextHelper securityContextHelper;

    /**
     * Проверить что пользователь является GLOBAL_ADMIN.
     * Выбрасывает ForbiddenAccessException если нет доступа.
     */
    public void checkGlobalAdminAccess() {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        if (!securityContextHelper.hasRole(Role.ROLE_GLOBAL_ADMIN)) {
            throw new ForbiddenAccessException("Доступен только для GLOBAL_ADMIN");
        }
    }

    /**
     * Проверить что пользователь является BAR_OWNER или BREW_OWNER.
     * Выбрасывает ForbiddenAccessException если нет доступа.
     */
    public void checkOwnerAccess() {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        if (!user.isOwner()) {
            throw new ForbiddenAccessException("Доступен только для BAR_OWNER и BREW_OWNER");
        }
    }

    /**
     * Проверить что пользователь является GLOBAL_ADMIN, BAR_OWNER или BREW_OWNER.
     * Выбрасывает ForbiddenAccessException если нет доступа.
     */
    public void checkGlobalAdminOrOwnerAccess() {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        boolean hasAccess = securityContextHelper.hasRole(Role.ROLE_GLOBAL_ADMIN) || user.isOwner();
        if (!hasAccess) {
            throw new ForbiddenAccessException("Доступ запрещён");
        }
    }

    /**
     * Проверить что пользователь является GLOBAL_ADMIN или BAR_OWNER.
     */
    public void checkGlobalAdminOrBarOwnerAccess() {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        boolean hasAccess = securityContextHelper.hasRole(Role.ROLE_GLOBAL_ADMIN) || user.isBarOwner();
        if (!hasAccess) {
            throw new ForbiddenAccessException("Доступ запрещён");
        }
    }

    /**
     * Проверить что пользователь является GLOBAL_ADMIN или BREW_OWNER.
     */
    public void checkGlobalAdminOrBrewOwnerAccess() {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        boolean hasAccess = securityContextHelper.hasRole(Role.ROLE_GLOBAL_ADMIN) || user.isBrewOwner();
        if (!hasAccess) {
            throw new ForbiddenAccessException("Доступ запрещён");
        }
    }

    /**
     * Проверить что пользователь является менеджером указанного заведения.
     * Выбрасывает ForbiddenAccessException если нет доступа.
     */
    public void checkManagerAccess(Long venueId) {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        String externalIdStr = user.getExternalId();
        
        // TODO: Здесь будет проверка через VenueMemberRepository
        // VenueMember member = venueMemberRepository.findByVenueIdAndUserId(venueId, userId);
        // if (member == null || !"MANAGER".equals(member.getRoleType())) {
        //     throw new ForbiddenAccessException("Вы не являетесь менеджером этого заведения");
        // }
        
        // Временная заглушка — пока пропускаем всех
        if (venueId == null) {
            throw new ForbiddenAccessException("venueId не указан");
        }
    }

    /**
     * Проверить что пользователь является владельцем или менеджером заведения.
     */
    public void checkVenueAccess(Long venueId) {
        AuthenticatedUser user = securityContextHelper.getCurrentUser();
        
        // Владелец (BAR_OWNER/BREW_OWNER) автоматически имеет доступ к своему venueId
        if (user.isOwner() && user.getVenueId() != null && user.getVenueId().equals(venueId)) {
            return;
        }
        
        // Менеджер проверяется отдельно
        checkManagerAccess(venueId);
    }

    /**
     * Проверить что пользователь имеет указанную роль.
     */
    public void checkRole(Role requiredRole) {
        if (!securityContextHelper.hasRole(requiredRole)) {
            throw new ForbiddenAccessException("Требуемая роль: " + requiredRole);
        }
    }

    /**
     * Проверить что пользователь аутентифицирован.
     */
    public void checkAuthenticated() {
        AuthenticatedUser user = securityContextHelper.getAuthenticatedUser();
        if (user == null) {
            throw new IllegalStateException("Пользователь не аутентифицирован");
        }
    }
}
