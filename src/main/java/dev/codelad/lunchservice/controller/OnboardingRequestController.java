package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.response.OnboardingRequestResponse;
import dev.codelad.lunchservice.entity.User;
import dev.codelad.lunchservice.repository.UserRepository;
import dev.codelad.lunchservice.security.Role;
import dev.codelad.lunchservice.security.SecurityContextHelper;
import dev.codelad.lunchservice.service.OnboardingRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для управления заявками на присоединение к системе Ланчей.
 * Пользователь подает заявку → GL одобряет → пользователь получает роль FRANCHISE_OWNER.
 */
@Slf4j
@Tag(name = ApiTags.ONBOARDING, description = ApiTags.ONBOARDING_DESC)
@RestController
@RequestMapping("/api/v1/lunch/onboarding")
@RequiredArgsConstructor
public class OnboardingRequestController {

    private final OnboardingRequestService onboardingRequestService;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    // ==================== USER: управление своими заявками ====================

    @PostMapping
    @Operation(
            summary = "Подать заявку на присоединение к системе",
            description = "Пользователь подает заявку на присоединение к системе Ланчей. " +
                    "Заявка отправляется на рассмотрение Global Admin.",
            tags = {ApiTags.ONBOARDING, ApiTags.USER}
    )
    @ApiResponse(responseCode = "201", description = "Заявка успешно создана")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации или уже есть активная заявка")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OnboardingRequestResponse> create() {
        // Сохраняем externalId (userUuid из JWT), а не внутренний ID из БД
        UUID externalId = securityContextHelper.getCurrentExternalId();
        if (externalId == null) {
            throw new IllegalStateException("Не удалось получить externalId пользователя");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onboardingRequestService.create(externalId));
    }

    @GetMapping("/my")
    @Operation(
            summary = "Получить мои заявки",
            description = "Возвращает список всех заявок текущего пользователя. Доступно для всех аутентифицированных пользователей.",
            tags = {ApiTags.ONBOARDING, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Список заявок")
    @PreAuthorize("isAuthenticated()")
    public List<OnboardingRequestResponse> findMyApplications() {
        UUID externalId = securityContextHelper.getCurrentExternalId();
        log.info("[ONBOARDING] Получение заявок для externalId: {}", externalId);
        if (externalId == null) {
            log.warn("[ONBOARDING] externalId не найден в SecurityContext");
            return List.of();
        }
        return onboardingRequestService.findByUserId(externalId);
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(
            summary = "Отменить свою заявку",
            description = "Пользователь может отменить свою заявку до её рассмотрения. Доступно для всех аутентифицированных пользователей.",
            tags = {ApiTags.ONBOARDING, ApiTags.USER}
    )
    @ApiResponse(responseCode = "204", description = "Заявка отменена")
    @ApiResponse(responseCode = "404", description = "Заявка не найдена")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelMyApplication(@PathVariable Long id) {
        UUID externalId = securityContextHelper.getCurrentExternalId();
        log.info("[ONBOARDING] Отмена заявки {} для externalId: {}", id, externalId);
        onboardingRequestService.cancelByUser(id, externalId);
        return ResponseEntity.noContent().build();
    }

    // ==================== ADMIN: рассмотрение заявок ====================

    @GetMapping("/pending")
    @Operation(
            summary = "Получить ожидающие заявки",
            description = "Возвращает список всех заявок в статусе PENDING для рассмотрения.",
            tags = {ApiTags.ONBOARDING, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Список ожидающих заявок")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public List<OnboardingRequestResponse> findPendingApplications() {
        return onboardingRequestService.findPendingApplications();
    }

    @PutMapping("/{id}/approve")
    @Operation(
            summary = "Одобрить заявку",
            description = "Одобряет заявку и автоматически назначает пользователя FRANCHISE_OWNER.",
            tags = {ApiTags.ONBOARDING, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Заявка одобрена, пользователь назначен FRANCHISE_OWNER")
    @ApiResponse(responseCode = "404", description = "Заявка не найдена")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<OnboardingRequestResponse> approve(@PathVariable Long id) {
        UUID userId = securityContextHelper.getCurrentUserId();
        String email = securityContextHelper.getCurrentUserEmail();
        Role role = securityContextHelper.getCurrentUserRole();

        log.info("[ONBOARDING] Одобрение заявки {} пользователем: email={}, role={}", id, email, role);

        return ResponseEntity.ok(onboardingRequestService.approve(id, userId));
    }

    @PutMapping("/{id}/reject")
    @Operation(
            summary = "Отклонить заявку",
            description = "Отклоняет заявку с комментарием.",
            tags = {ApiTags.ONBOARDING, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Заявка отклонена")
    @ApiResponse(responseCode = "404", description = "Заявка не найдена")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<OnboardingRequestResponse> reject(
            @PathVariable Long id,
            @Parameter(description = "Комментарий к отклонению")
            @RequestParam(required = false) String comment) {
        UUID userId = securityContextHelper.getCurrentUserId();
        String email = securityContextHelper.getCurrentUserEmail();
        Role role = securityContextHelper.getCurrentUserRole();

        log.info("[ONBOARDING] Отклонение заявки {} пользователем: email={}, role={}, comment={}",
                id, email, role, comment);

        return ResponseEntity.ok(onboardingRequestService.reject(id, userId, comment));
    }

    // ==================== ADMIN: назначение LOCATION_ADMIN ====================

    @PostMapping("/assign-location-admin")
    @Operation(
            summary = "Назначить локального администратора",
            description = "GL или FO назначает пользователя локальным администратором. " +
                    "Записывает кто назначил (reviewedBy) и когда (reviewedAt).",
            tags = {ApiTags.ONBOARDING, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Пользователь назначен LOCATION_ADMIN")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    @PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'FRANCHISE_OWNER')")
    public ResponseEntity<Void> assignLocationAdmin(
            @Parameter(description = "UUID пользователя для назначения", required = true)
            @RequestParam UUID userId) {
        String email = securityContextHelper.getCurrentUserEmail();
        Role role = securityContextHelper.getCurrentUserRole();

        // Проверка: только GL или FO могут назначать
        if (!securityContextHelper.isAdmin() && !securityContextHelper.isFranchiseOwner()) {
            throw new IllegalStateException("Только GLOBAL_ADMIN или FRANCHISE_OWNER может назначать LOCATION_ADMIN");
        }

        User user = userRepository.findByExternalId(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.setRole(Role.ROLE_LOCATION_ADMIN);
        userRepository.save(user);

        log.info("[ONBOARDING] Пользователь {} назначен LOCATION_ADMIN, назначил: email={}, role={}",
                userId, email, role);

        return ResponseEntity.ok().build();
    }
}
