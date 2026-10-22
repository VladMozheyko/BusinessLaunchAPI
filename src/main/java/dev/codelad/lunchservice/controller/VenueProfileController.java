package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.VenueProfileRequest;
import dev.codelad.lunchservice.dto.response.VenueProfileResponse;
import dev.codelad.lunchservice.security.PermissionService;
import dev.codelad.lunchservice.security.Role;
import dev.codelad.lunchservice.security.SecurityContextHelper;
import dev.codelad.lunchservice.service.VenueProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Контроллер для управления карточками заведений.
 * Франчайзи создают и редактируют карточки своих бизнес-ланчей.
 */
@Slf4j
@Tag(name = ApiTags.VENUE_PROFILES, description = ApiTags.VENUE_PROFILES_DESC)
@RestController
@RequestMapping("/api/v1/lunch/venue")
@RequiredArgsConstructor
public class VenueProfileController {

    private final VenueProfileService venueProfileService;
    private final SecurityContextHelper securityContextHelper;
    private final PermissionService permissionService;

    @PostMapping
    @Operation(
            summary = "Создать или обновить карточку заведения",
            description = "Создаёт новую карточку бизнес-ланча или обновляет существующую. " +
                    "Включает название, описание, логотип и расписание работы по дням недели.",
            tags = {ApiTags.VENUE_PROFILES, ApiTags.FRANCHISE_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Карточка создана или обновлена")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    public ResponseEntity<VenueProfileResponse> createOrUpdate(
            @Parameter(description = "Данные карточки заведения", required = true)
            @Valid @RequestBody VenueProfileRequest request) {
        permissionService.checkOwnerAccess();
        String externalIdStr = securityContextHelper.getCurrentExternalId();

        log.info("[VENUE] Создание/обновление карточки пользователем: externalId={}", externalIdStr);
        return ResponseEntity.ok(venueProfileService.createOrUpdate(externalIdStr, request));
    }

    @GetMapping("/my")
    @Operation(
            summary = "Получить карточку моего заведения",
            description = "Возвращает активную карточку заведения текущего франчайзи. " +
                    "Не требует указания ID — возвращается карточка, привязанная к текущему пользователю.",
            tags = {ApiTags.VENUE_PROFILES, ApiTags.FRANCHISE_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Карточка найдена")
    @ApiResponse(responseCode = "404", description = "Карточка не найдена")
    public ResponseEntity<VenueProfileResponse> getMyVenue() {
        permissionService.checkOwnerAccess();
        String externalIdStr = securityContextHelper.getCurrentExternalId();

        log.info("[VENUE] Просмотр карточки заведения пользователем: externalId={}", externalIdStr);
        return ResponseEntity.ok(venueProfileService.getByOwnerUserId(externalIdStr));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить карточку заведения по ID",
            description = "Возвращает карточку заведения по уникальному ID. " +
                    "Доступно для Global Admin для просмотра любых карточек.",
            tags = {ApiTags.VENUE_PROFILES, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Карточка найдена")
    @ApiResponse(responseCode = "404", description = "Карточка не найдена")
    public ResponseEntity<VenueProfileResponse> getVenueById(@PathVariable Long id) {
        permissionService.checkGlobalAdminAccess();
        String email = securityContextHelper.getCurrentUserEmail();
        Role role = securityContextHelper.getCurrentUserRole();

        log.info("[VENUE] Просмотр карточки заведения {} глобальным админом: email={}, role={}",
                id, email, role);

        return ResponseEntity.ok(venueProfileService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить карточку заведения",
            description = "Полное удаление карточки заведения. " +
                    "Доступно только владельцу карточки (FRANCHISE_OWNER) и глобальному админу (GLOBAL_ADMIN).",
            tags = {ApiTags.VENUE_PROFILES, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER}
    )
    @ApiResponse(responseCode = "204", description = "Карточка удалена")
    @ApiResponse(responseCode = "404", description = "Карточка не найдена")
    public ResponseEntity<Void> deleteVenueProfile(
            @Parameter(description = "ID карточки заведения", required = true)
            @PathVariable Long id) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        venueProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
