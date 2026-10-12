package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.dto.request.CreateComboRequest;
import dev.codelad.lunchservice.dto.request.UpdateComboStatusRequest;
import dev.codelad.lunchservice.dto.response.ComboFullResponse;
import dev.codelad.lunchservice.dto.response.ComboResponse;
import dev.codelad.lunchservice.entity.Combo;
import dev.codelad.lunchservice.service.ComboService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для управления комбо-наборами.
 */
@Tag(name = ApiTags.COMBOS, description = ApiTags.COMBOS_DESC)
@RestController
@RequestMapping("/api/v1/lunch")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

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
                    Доступно только для GL и FO.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER}
    )
    @ApiResponse(responseCode = "201", description = "Комбо успешно создано")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER')")
    public ResponseEntity<ComboResponse> createCombo(
            @Parameter(description = "Данные для создания комбо", required = true)
            @Valid @RequestBody CreateComboRequest request) {
        Combo combo = comboService.createCombo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ComboResponse.from(combo));
    }

    @GetMapping("/combo/my")
    @Operation(
            summary = "Получить все мои комбо для заведения",
            description = """
                    Возвращает все комбо для указанного заведения Франчайзи.
                    Доступно для GL, FO и LA.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Список комбо")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public List<ComboResponse> findMyCombosByVenueId(
            @Parameter(description = "ID заведения", required = true)
            @RequestParam Long venueId) {
        return comboService.findByVenueId(venueId);
    }

    @PatchMapping("/combo/{id}/status")
    @Operation(
            summary = "Изменение активности комбо",
            description = """
                    Включает или выключает комбо-набор.
                    Доступно для GL, FO и LA.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Статус комбо обновлён")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<ComboResponse> updateComboStatus(
            @Parameter(description = "ID комбо", required = true)
            @PathVariable Long id,
            @Parameter(description = "Новое значение isActive", required = true)
            @Valid @RequestBody UpdateComboStatusRequest request) {
        Combo combo = comboService.updateComboStatus(id, request);
        return ResponseEntity.ok(ComboResponse.from(combo));
    }

    @DeleteMapping("/combo/{id}")
    @Operation(
            summary = "Удаление комбо",
            description = """
                    Полное удаление комбо-набора.
                    Нельзя удалить комбо с активными заказами (PENDING, APPROVED).
                    Сначала установите isActive = false, дождитесь выполнения заказов, затем удалите.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN, ApiTags.FRANCHISE_OWNER, ApiTags.LOCATION_ADMIN}
    )
    @ApiResponse(responseCode = "204", description = "Комбо удалено")
    @ApiResponse(responseCode = "404", description = "Комбо не найдено")
    @ApiResponse(responseCode = "409", description = "Нельзя удалить: есть активные заказы")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_FRANCHISE_OWNER', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<Void> deleteCombo(
            @Parameter(description = "ID комбо", required = true)
            @PathVariable Long id) {
        comboService.deleteCombo(id);
        return ResponseEntity.noContent().build();
    }
}
