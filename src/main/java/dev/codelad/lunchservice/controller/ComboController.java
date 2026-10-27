package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.CreateComboRequest;
import dev.codelad.lunchservice.dto.request.UpdateComboRequest;
import dev.codelad.lunchservice.dto.request.UpdateComboStatusRequest;
import dev.codelad.lunchservice.dto.response.ComboFullResponse;
import dev.codelad.lunchservice.dto.response.ComboResponse;
import dev.codelad.lunchservice.entity.Combo;
import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.security.PermissionService;
import dev.codelad.lunchservice.security.SecurityContextHelper;
import dev.codelad.lunchservice.service.ComboService;
import dev.codelad.lunchservice.service.VenueProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для управления комбо-наборами.
 */
@Slf4j
@Tag(name = ApiTags.COMBOS, description = ApiTags.COMBOS_DESC)
@RestController
@RequestMapping("/api/v1/lunch")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;
    private final VenueProfileService venueProfileService;
    private final SecurityContextHelper securityContextHelper;
    private final PermissionService permissionService;

    @GetMapping("/combo")
    @Operation(
            summary = "Получить все комбо с деталями",
            description = """
                    Возвращает все комбо-наборы с категориями, блюдами и дополнениями.
                    Доступно всем, включая неаутентифицированных пользователей.
                """,
            tags = {ApiTags.COMBOS}
    )
    @ApiResponse(responseCode = "200", description = "Список комбо получен")
    public List<ComboFullResponse> getAllCombosWithDetails() {
        return comboService.findAllWithDetails();
    }

    @GetMapping("/combo/{id}")
    @Operation(
            summary = "Получить комбо с деталями",
            description = """
                        Возвращает одно комбо-набор с категориями, блюдами и дополнениями.
                        Доступно всем, включая неаутентифицированных пользователей.
                    """,
            tags = {ApiTags.COMBOS}
    )
    @ApiResponse(responseCode = "200", description = "Комбо найдено")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    public ResponseEntity<ComboFullResponse> getComboWithDetails(@PathVariable Long id) {
        return ResponseEntity.ok(comboService.findByIdWithDetails(id));
    }

    @PostMapping("/combo")
    @Operation(
            summary = "Создание комбо",
            description = """
                    Создаёт новый комбо-набор.
                    Доступно только для GL и BAR/BREW OWNER.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.BAR_BREW_OWNER}
    )
    @ApiResponse(responseCode = "201", description = "Комбо успешно создано")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    public ResponseEntity<ComboResponse> createCombo(
            @Parameter(description = "Данные для создания комбо", required = true)
            @Valid @RequestBody CreateComboRequest request) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        Long venueId = securityContextHelper.getAuthenticatedUser().getVenueId();
        VenueProfile venueProfile = venueProfileService.getOrCreateVenueProfile(venueId);
        Combo combo = comboService.createCombo(request, venueProfile);
        return ResponseEntity.status(HttpStatus.CREATED).body(ComboResponse.from(combo));
    }

    @GetMapping("/combo/my")
    @Operation(
            summary = "Получить все мои комбо для заведения",
            description = """
                    Возвращает все комбо для указанного заведения.
                    Доступно для GL, BAR/BREW OWNER и LM.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.BAR_BREW_OWNER, ApiTags.LOCATION_MANAGER}
    )
    @ApiResponse(responseCode = "200", description = "Список комбо")
    public List<ComboResponse> findMyCombosByVenueId() {
        permissionService.checkGlobalAdminOrOwnerAccess();
        Long venueId = securityContextHelper.getAuthenticatedUser().getVenueId();
        VenueProfile venueProfile = venueProfileService.getOrCreateVenueProfile(venueId);

        log.info("[COMBO] Просмотр списка комбо пользователем: venueId={}", venueId);
        return comboService.findByVenueProfile(venueProfile);
    }

    @PatchMapping("/combo/{id}/status")
    @Operation(
            summary = "Изменение активности комбо",
            description = """
                    Включает или выключает комбо-набор.
                    Доступно для GL, BAR/BREW OWNER и LM.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.BAR_BREW_OWNER, ApiTags.LOCATION_MANAGER}
    )
    @ApiResponse(responseCode = "200", description = "Статус комбо обновлён")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    public ResponseEntity<ComboResponse> updateComboStatus(
            @Parameter(description = "ID комбо", required = true)
            @PathVariable Long id,
            @Parameter(description = "Новое значение isActive", required = true)
            @Valid @RequestBody UpdateComboStatusRequest request) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        Combo combo = comboService.updateComboStatus(id, request);
        return ResponseEntity.ok(ComboResponse.from(combo));
    }

    @PatchMapping("/combo/{id}")
    @Operation(
            summary = "Обновление параметров комбо",
            description = """
                    Обновляет название, цену и/или дневной лимит комбо.
                    Доступно для GL, BAR/BREW OWNER и LM.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.BAR_BREW_OWNER, ApiTags.LOCATION_MANAGER}
    )
    @ApiResponse(responseCode = "200", description = "Комбо обновлено")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    public ResponseEntity<ComboResponse> updateCombo(
            @Parameter(description = "ID комбо", required = true)
            @PathVariable Long id,
            @Parameter(description = "Новые значения name, price, dailyLimit", required = true)
            @Valid @RequestBody UpdateComboRequest request) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        Combo combo = comboService.updateCombo(id, request);
        return ResponseEntity.ok(ComboResponse.from(combo));
    }

    @DeleteMapping("/combo/{id}")
    @Operation(
            summary = "Скрыть комбо (мягкое удаление)",
            description = """
                    Скрывает комбо-набор (устанавливает isHidden=true).
                    Нельзя скрыть комбо с активными заказами.
                    Доступно для GL, BAR/BREW OWNER и LM.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.BAR_BREW_OWNER, ApiTags.LOCATION_MANAGER}
    )
    @ApiResponse(responseCode = "204", description = "Комбо скрыто")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    @ApiResponse(responseCode = "409", description = "Нельзя скрыть: есть активные заказы")
    public ResponseEntity<Void> hideCombo(
            @Parameter(description = "ID комбо", required = true)
            @PathVariable Long id) {
        permissionService.checkGlobalAdminOrOwnerAccess();
        comboService.hideCombo(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/combo/{id}/hard")
    @Operation(
            summary = "Физическое удаление комбо",
            description = """
                    Полное физическое удаление комбо-набора.
                    Нельзя удалить комбо с активными заказами.
                    Только для GLOBAL_ADMIN.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "204", description = "Комбо физически удалено")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    @ApiResponse(responseCode = "409", description = "Нельзя удалить: есть активные заказы или комбо скрыто")
    public ResponseEntity<Void> hardDeleteCombo(
            @Parameter(description = "ID комбо", required = true)
            @PathVariable Long id) {
        permissionService.checkGlobalAdminAccess();
        comboService.hardDeleteCombo(id);
        return ResponseEntity.noContent().build();
    }
}
