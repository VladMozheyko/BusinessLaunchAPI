package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.entity.FoodSyncStatus;
import dev.codelad.lunchservice.security.PermissionService;
import dev.codelad.lunchservice.service.FoodSyncService;
import dev.codelad.lunchservice.service.FoodSyncService.FoodSyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Контроллер для управления синхронизацией данных из Food Service (кассовый аппарат).
 * Предоставляет эндпоинты для получения сырых данных и запуска синхронизации.
 */
@Slf4j
@Tag(name = ApiTags.FOOD_SYNC, description = ApiTags.FOOD_SYNC_DESC)
@RestController
@RequestMapping("/api/v1/lunch/food-sync")
@RequiredArgsConstructor
public class FoodSyncController {

    private final FoodSyncService foodSyncService;
    private final PermissionService permissionService;

    /**
     * Запустить синхронизацию меню из Food Service.
     * Вызывается при входе BAR_OWNER/BREW_OWNER или по запросу.
     */
    @PostMapping("/sync/{venueId}")
    @Operation(
            summary = "Запустить синхронизацию меню из Food Service",
            description = "Подтягивает категории и блюда из кассового аппарата (Food Service). " +
                    "Локальные данные пользователя (syncedFromFood=false) сохраняются. " +
                    "Синхронизированные данные (syncedFromFood=true) обновляются. " +
                    "Новые категории и блюда создаются с hidden=true. " +
                    "Также обновляет логотип заведения.",
            tags = {ApiTags.FOOD_SYNC, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Синхронизация завершена успешно")
    public ResponseEntity<FoodSyncResult> syncMenu(
            @Parameter(description = "ID заведения", required = true)
            @PathVariable Long venueId) {
        
        permissionService.checkOwnerAccess();
        
        log.info("Запуск синхронизации меню для venueId={}", venueId);
        FoodSyncResult result = foodSyncService.syncMenu(venueId, "MANUAL");
        
        if (result.isSuccess()) {
            log.info("Синхронизация меню для venueId={} завершена успешно: {}", venueId, result);
        } else {
            log.warn("Синхронизация меню для venueId={} завершена с ошибкой: {}", venueId, result.error());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Запустить синхронизацию только логотипа из Food Service.
     * Вызывается когда нужно обновить только логотип без категорий/блюд.
     */
    @PostMapping("/sync-logo/{venueId}")
    @Operation(
            summary = "Запустить синхронизацию логотипа из Food Service",
            description = "Подтягивает только логотип заведения из кассового аппарата (Food Service). " +
                    "Категории и блюда не обнов��яются. " +
                    "Используется когда нужно обновить только логотип без изменения меню.",
            tags = {ApiTags.FOOD_SYNC, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Логотип успешно обновлён")
    @ApiResponse(responseCode = "404", description = "Заведение не найдено")
    public ResponseEntity<LogoSyncResponse> syncLogo(
            @Parameter(description = "ID заведения", required = true)
            @PathVariable Long venueId) {
        
        permissionService.checkOwnerAccess();
        
        log.info("Запуск синхронизации логотипа для venueId={}", venueId);
        
        try {
            boolean updated = foodSyncService.syncLogoOnly(venueId);
            
            if (updated) {
                log.info("Синхронизация логотипа для venueId={} завершена успешно", venueId);
            } else {
                log.warn("Логотип не обновлён для venueId={} (не изменился или отсутствует)", venueId);
            }
            
            return ResponseEntity.ok(new LogoSyncResponse(venueId, updated));
            
        } catch (Exception e) {
            log.error("Ошибка синхронизации логотипа для venueId={}: {}", venueId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new LogoSyncResponse(venueId, false, e.getMessage()));
        }
    }

    /**
     * Получить статус синхронизации для заведения.
     */
    @GetMapping("/status/{venueId}")
    @Operation(
            summary = "Получить статус синхронизации меню",
            description = "Возвращает информацию о последней синхронизации: время, количество категорий/блюд, тип.",
            tags = {ApiTags.FOOD_SYNC, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "200", description = "Статус синхронизации получен")
    public ResponseEntity<FoodSyncStatusResponse> getSyncStatus(
            @Parameter(description = "ID заведения", required = true)
            @PathVariable Long venueId) {
        
        permissionService.checkOwnerAccess();
        
        log.info("Запрос статуса синхронизации для venueId={}", venueId);
        
        FoodSyncStatus status = foodSyncService.getSyncStatus(venueId);
        
        if (status == null) {
            return ResponseEntity.ok(new FoodSyncStatusResponse(
                    venueId,
                    null,
                    0,
                    0,
                    "Синхронизация ещё не проводилась",
                    false
            ));
        }

        boolean needsSync = foodSyncService.needsSync(venueId);

        return ResponseEntity.ok(new FoodSyncStatusResponse(
                venueId,
                formatInstant(status.getLastSyncTime()),
                status.getSyncedCategoriesCount() != null ? status.getSyncedCategoriesCount() : 0,
                status.getSyncedDishesCount() != null ? status.getSyncedDishesCount() : 0,
                status.getErrorMessage(),
                needsSync
        ));
    }

    /**
     * Форматирование Instant в читаемую строку.
     */
    private String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.of("Europe/Moscow"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Response DTO для статуса синхронизации.
     */
    public record FoodSyncStatusResponse(
            Long venueId,
            String lastSyncTime,
            int syncedCategoriesCount,
            int syncedDishesCount,
            String errorMessage,
            boolean needsSync
    ) {
        public boolean isSuccess() {
            return errorMessage == null;
        }
    }

    /**
     * Response DTO для синхронизации логотипа.
     */
    public record LogoSyncResponse(
            Long venueId,
            boolean updated,
            String errorMessage
    ) {
        // Конструктор для успешного случая
        public LogoSyncResponse(Long venueId, boolean updated) {
            this(venueId, updated, null);
        }
        
        public boolean isSuccess() {
            return errorMessage == null;
        }
    }
}
