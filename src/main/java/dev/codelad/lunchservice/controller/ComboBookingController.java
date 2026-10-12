package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.security.limiter.InMemoryRateLimiter;
import dev.codelad.lunchservice.dto.request.CancelByCodeRequest;
import dev.codelad.lunchservice.dto.request.CancelByPhoneRequest;
import dev.codelad.lunchservice.dto.request.CreateComboBookingRequest;
import dev.codelad.lunchservice.dto.response.ComboBookingResponse;
import dev.codelad.lunchservice.entity.ComboBooking;
import dev.codelad.lunchservice.service.ComboBookingService;
import dev.codelad.lunchservice.util.IpAddressUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для уп��авления бронированиями комбо-наборов.
 */
@Tag(name = ApiTags.COMBOS, description = "Управление комбо-наборами и бронированиями")
@RestController
@RequestMapping("/api/v1/lunch")
@RequiredArgsConstructor
public class ComboBookingController {

    private final ComboBookingService comboBookingService;
    private final InMemoryRateLimiter rateLimiter;

    @PostMapping("/combo-booking")
    @Operation(
            summary = "Создание бронирования комбо",
            description = """
                    Создаёт бронирование комбо-набора с указанием количества, дополнений,
                    имени, телефона и комментария.
                    Доступно всем.
                """,
            tags = {ApiTags.COMBOS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "201", description = "Бронирование успешно создано")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Заведение или комбо не найдено")
    @ApiResponse(responseCode = "409", description = "Нет мест или комбо недоступно")
    public ResponseEntity<ComboBookingResponse> createComboBooking(
            @Parameter(description = "Данные для бронирования", required = true)
            @Valid @RequestBody CreateComboBookingRequest request) {
        ComboBooking booking = comboBookingService.createComboBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ComboBookingResponse.from(booking));
    }

    @PostMapping("/combo-booking/cancel-by-code")
    @Operation(
            summary = "Отмена бронирования комбо по коду",
            description = """
                    Отменяет бронирование по уникальному коду отмены.
                    Формат кода: XXX-XXX-XXX.
                    Защищено rate limiting: 5 попыток в минуту на IP.
                """,
            tags = {ApiTags.COMBOS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование успешно отменено")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование уже отменено или выполнено")
    @ApiResponse(responseCode = "429", description = "Превышен лимит запросов (5 попыток в минуту)")
    public ResponseEntity<ComboBookingResponse> cancelByCode(
            @Parameter(description = "Запрос с кодом отмены", required = true)
            @Valid @RequestBody CancelByCodeRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest) {

        // Rate limiting по IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkLimit(clientIp);

        ComboBooking cancelled = comboBookingService.cancelByCode(request.getCancelCode());
        return ResponseEntity.ok(ComboBookingResponse.from(cancelled));
    }

    @PostMapping("/combo-booking/cancel-by-code-and-phone")
    @Operation(
            summary = "Отмена бронирования комбо по коду и телефону",
            description = """
                    Отменяет бронирование по коду и номеру телефона.
                    Гость вводит код, полученный при создании брони, и номер телефона.
                    Формат кода: XXX-XXX-XXX
                    Защищено rate limiting: 5 попыток в минуту на IP.
                """,
            tags = {ApiTags.COMBOS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование успешно отменено")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено или номер не совпадает")
    @ApiResponse(responseCode = "409", description = "Бронирование уже отменено или выполнено")
    @ApiResponse(responseCode = "429", description = "Превышен лимит запросов (5 попыток в минуту)")
    public ResponseEntity<ComboBookingResponse> cancelByCodeAndPhone(
            @Parameter(description = "Запрос с кодом отмены и номером телефона", required = true)
            @Valid @RequestBody CancelByPhoneRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest) {

        // Rate limiting по IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkLimit(clientIp);

        ComboBooking cancelled = comboBookingService.cancelByCodeAndPhone(request.getCancelCode(), request.getPhoneNumber());
        return ResponseEntity.ok(ComboBookingResponse.from(cancelled));
    }

    @GetMapping("/combo-booking/by-phone")
    @Operation(
            summary = "Мои бронирования по телефону",
            description = """
                    Возвращает список всех бронирований комбо по номеру телефона.
                    Доступно всем, включая неаутентифицированных пользователей.
                """,
            tags = {ApiTags.COMBOS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Список бронирований получен")
    public ResponseEntity<List<ComboBookingResponse>> getBookingsByPhone(
            @Parameter(description = "Номер телефона", required = true)
            @RequestParam String phone) {
        List<ComboBooking> bookings = comboBookingService.getBookingsByPhone(phone);
        List<ComboBookingResponse> responses = bookings.stream()
                .map(ComboBookingResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/combo-booking/my")
    @Operation(
            summary = "Мои бронирования комбо",
            description = """
                    Возвращает список всех бронирований комбо текущего пользователя.
                    Только для аутентифицированных пользователей.
                """,
            tags = {ApiTags.COMBOS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Список бронирований получен")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComboBookingResponse>> getMyComboBookings() {
        List<ComboBooking> bookings = comboBookingService.getUserBookings();
        List<ComboBookingResponse> responses = bookings.stream()
                .map(ComboBookingResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/combo-booking/{id}/approve")
    @Operation(
            summary = "Подтверждение бронирования комбо",
            description = """
                    Подтверждает бронирование, переводя его в статус APPROVED.
                    Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование подтверждено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование не в статусе PENDING")
    @PreAuthorize("hasAnyAuthority('ROLE_FRANCHISE_OWNER', 'ROLE_GLOBAL_ADMIN', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<ComboBookingResponse> approveComboBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable java.util.UUID id) {
        ComboBooking approved = comboBookingService.approveBooking(id);
        return ResponseEntity.ok(ComboBookingResponse.from(approved));
    }

    @PostMapping("/combo-booking/{id}/complete")
    @Operation(
            summary = "Выполнение бронирования комбо",
            description = """
                    Завершает бронирование, переводя его в статус COMPLETED.
                    Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование выполнено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование не в статусе APPROVED")
    @PreAuthorize("hasAnyAuthority('ROLE_FRANCHISE_OWNER', 'ROLE_GLOBAL_ADMIN', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<ComboBookingResponse> completeComboBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable java.util.UUID id) {
        ComboBooking completed = comboBookingService.completeBooking(id);
        return ResponseEntity.ok(ComboBookingResponse.from(completed));
    }

    @PostMapping("/combo-booking/{id}/cancel")
    @Operation(
            summary = "Отмена бронирования комбо админом",
            description = """
                    Отменяет бронирование из любого статуса (кроме CANCELLED и COMPLETED).
                    Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
                """,
            tags = {ApiTags.COMBOS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование отменено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование уже отменено или выполнено")
    @PreAuthorize("hasAnyAuthority('ROLE_FRANCHISE_OWNER', 'ROLE_GLOBAL_ADMIN', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<ComboBookingResponse> cancelComboBookingByAdmin(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable java.util.UUID id) {
        ComboBooking cancelled = comboBookingService.cancelByAdmin(id);
        return ResponseEntity.ok(ComboBookingResponse.from(cancelled));
    }

    /**
     * Извлечь IP-адрес клиента из запроса (учитывает proxy).
     */
    private String getClientIp(HttpServletRequest request) {
        return IpAddressUtils.getClientIp(request);
    }
}
