package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.UpdateVenueSettingsRequest;
import dev.codelad.lunchservice.dto.response.VenueSettingsResponse;
import dev.codelad.lunchservice.entity.ComboBooking;
import dev.codelad.lunchservice.entity.LunchBooking;
import dev.codelad.lunchservice.security.PermissionService;
import dev.codelad.lunchservice.service.VenueSettingsService;
import dev.codelad.lunchservice.task.BookingCleanupTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Контроллер для управления настройками заведения.
 * Предоставляет эндпоинты для получения и обновления настроек,
 * а также для ручного восстановления NO_SHOW бронирований.
 */
@Slf4j
@Tag(name = ApiTags.VENUE_SETTINGS, description = ApiTags.VENUE_SETTINGS_DESC)
@RestController
@RequestMapping("/api/owner/settings")
@RequiredArgsConstructor
public class VenueSettingsController {

    private final VenueSettingsService venueSettingsService;
    private final BookingCleanupTask bookingCleanupTask;
    private final PermissionService permissionService;

    // ==================== НАСТРОЙКИ ЗАВЕДЕНИЯ ====================

    /**
     * Получить настройки заведения.
     */
    @GetMapping("/{venueId}")
    @Operation(
            summary = "Получить настройки заведения",
            description = "Возвращает текущие настройки заведения: период ожидания no-show и другие параметры.",
            tags = {ApiTags.VENUE_SETTINGS, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Настройки получены")
    public ResponseEntity<VenueSettingsResponse> getSettings(
            @Parameter(description = "ID заведения", required = true)
            @PathVariable Long venueId) {

        permissionService.checkOwnerAccess();

        log.info("Запрос настроек для venueId={}", venueId);
        VenueSettingsResponse settings = venueSettingsService.getSettings(venueId);
        return ResponseEntity.ok(settings);
    }

    /**
     * Обновить настройки заведения.
     */
    @PutMapping("/{venueId}")
    @Operation(
            summary = "Обновить настройки заведения",
            description = "Обновляет параметры заведения. Например: период ожидания no-show (5-120 минут).",
            tags = {ApiTags.VENUE_SETTINGS, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Настройки обновлены")
    public ResponseEntity<VenueSettingsResponse> updateSettings(
            @Parameter(description = "ID заведения", required = true)
            @PathVariable Long venueId,
            @Valid @RequestBody UpdateVenueSettingsRequest request) {

        permissionService.checkOwnerAccess();

        log.info("Обновление настроек для venueId={}", venueId);
        VenueSettingsResponse settings = venueSettingsService.updateSettings(venueId, request);
        return ResponseEntity.ok(settings);
    }

    // ==================== ВОССТАНОВЛЕНИЕ NO_SHOW ====================

    /**
     * Восстановить lunch бронирование из NO_SHOW в APPROVED.
     * Вызывается менеджером, когда пользователь пришёл, но scheduler уже пометил как NO_SHOW.
     */
    @PostMapping("/restore/lunch/{bookingId}")
    @Operation(
            summary = "Восстановить lunch бронирование из NO_SHOW",
            description = "Ручное восстановление бронирования из статуса NO_SHOW в APPROVED. " +
                    "Используется, когда пользователь пришёл, но scheduler уже пометил как NO_SHOW.",
            tags = {ApiTags.VENUE_SETTINGS, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование восстановлено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "400", description = "Бронирование не в статусе NO_SHOW")
    public ResponseEntity<LunchBooking> restoreLunchBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable UUID bookingId) {

        permissionService.checkOwnerAccess();

        log.info("Восстановление lunch бронирования {} из NO_SHOW", bookingId);
        LunchBooking restored = bookingCleanupTask.restoreLunchBookingFromNoShow(bookingId);
        return ResponseEntity.ok(restored);
    }

    /**
     * Восстановить combo бронирование из NO_SHOW в APPROVED.
     */
    @PostMapping("/restore/combo/{bookingId}")
    @Operation(
            summary = "Восстановить combo бронирование из NO_SHOW",
            description = "Ручное восстановление combo-бронирования из статуса NO_SHOW в APPROVED. " +
                    "Используется, когда пользователь пришёл, но scheduler уже пометил как NO_SHOW.",
            tags = {ApiTags.VENUE_SETTINGS, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Combo бронирование восстановлено")
    @ApiResponse(responseCode = "404", description = "Combo бронирование не найдено")
    @ApiResponse(responseCode = "400", description = "Combo бронирование не в статусе NO_SHOW")
    public ResponseEntity<ComboBooking> restoreComboBooking(
            @Parameter(description = "ID combo бронирования", required = true)
            @PathVariable UUID bookingId) {

        permissionService.checkOwnerAccess();

        log.info("Восстановление combo бронирования {} из NO_SHOW", bookingId);
        ComboBooking restored = bookingCleanupTask.restoreComboBookingFromNoShow(bookingId);
        return ResponseEntity.ok(restored);
    }
}
