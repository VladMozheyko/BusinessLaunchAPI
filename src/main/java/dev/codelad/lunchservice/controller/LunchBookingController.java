package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.security.limiter.InMemoryRateLimiter;
import dev.codelad.lunchservice.dto.request.CancelByCodeRequest;
import dev.codelad.lunchservice.dto.request.CancelByPhoneRequest;
import dev.codelad.lunchservice.dto.request.CreateLunchBookingRequest;
import dev.codelad.lunchservice.dto.response.LunchBookingResponse;
import dev.codelad.lunchservice.entity.LunchBooking;
import dev.codelad.lunchservice.service.LunchBookingService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер для бронирования бизнес-ланчей.
 */
@Tag(name = ApiTags.LUNCH_BOOKINGS, description = ApiTags.LUNCH_BOOKINGS_DESC)
@RestController
@RequestMapping("/api/v1/lunch")
@RequiredArgsConstructor
public class LunchBookingController {

    private final LunchBookingService lunchBookingService;
    private final InMemoryRateLimiter rateLimiter;

    @PostMapping("/booking")
    @Operation(
            summary = "Создание бронирования ланча",
            description = """
                    Создаёт бронирование бизнес-ланча с указанием локации, времени и списка блюд.
                    Проверяет доступность мест и существование блюд.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "201", description = "Бронирование успешно создано")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Локация или блюдо не найдено")
    @ApiResponse(responseCode = "409", description = "Нет мест для бронирования")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LunchBookingResponse> createLunchBooking(
            @Parameter(description = "Данные для бронирования: локация, время, список блюд", required = true)
            @Valid @RequestBody CreateLunchBookingRequest request) {
        LunchBooking booking = lunchBookingService.createLunchBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LunchBookingResponse.from(booking));
    }

    @GetMapping("/booking/my")
    @Operation(
            summary = "Мои бронирования",
            description = """
                    Возвращает список всех бронирований текущего пользователя с общей суммой.
                    Отсортировано по дате создания (новые первые).
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Список бронирований получен")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LunchBookingResponse>> getMyBookings() {
        List<LunchBooking> bookings = lunchBookingService.getUserBookings();
        List<LunchBookingResponse> responses = bookings.stream()
                .map(LunchBookingResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/booking/{id}")
    @Operation(
            summary = "Просмотр бронирования",
            description = """
                    Возвращает детальную информацию о бронировании.
                    Пользователь может видеть только свои бронирования. Админ может видеть любые.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование найдено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено или нет доступа")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LunchBookingResponse> getBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable("id") java.util.UUID id) {
        LunchBooking booking = lunchBookingService.getBookingById(id);
        return ResponseEntity.ok(LunchBookingResponse.from(booking));
    }

    @PostMapping("/booking/cancel-by-code")
    @Operation(
            summary = "Отмена бронирования по коду",
            description = """
                    Отменяет бронирование по уникальному коду отмены.
                    Формат кода: XXX-XXX-XXX (например, A1B-2C3-D4E).
                    Код отправляется при создании бронирования.
                    Защищено rate limiting: 5 попыток в минуту на IP.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование успешно отменено")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование уже отменено")
    @ApiResponse(responseCode = "429", description = "Превышен лимит запросов (5 попыток в минуту)")
    public ResponseEntity<LunchBookingResponse> cancelByCode(
            @Parameter(description = "Запрос с кодом отмены", required = true)
            @Valid @RequestBody CancelByCodeRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest) {

        // Rate limiting по IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkLimit(clientIp);

        LunchBooking cancelled = lunchBookingService.cancelByCode(request.getCancelCode());
        return ResponseEntity.ok(LunchBookingResponse.from(cancelled));
    }

    @PostMapping("/booking/cancel-by-code-and-phone")
    @Operation(
            summary = "Отмена бронирования по коду и телефону",
            description = """
                    Отменяет бронирование по коду и номеру телефона.
                    Для незарегистрированных пользователей (гостей).
                    Гость вводит код, полученный при создании брони, и номер телефона.
                    Формат кода: XXX-XXX-XXX.
                    Защищено rate limiting: 5 попыток в минуту на IP.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.USER}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование успешно отменено")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено или номер не совпадает")
    @ApiResponse(responseCode = "409", description = "Бронирование уже отменено")
    @ApiResponse(responseCode = "429", description = "Превышен лимит запросов (5 попыток в минуту)")
    public ResponseEntity<LunchBookingResponse> cancelByCodeAndPhone(
            @Parameter(description = "Запрос с кодом отмены и номером телефона", required = true)
            @Valid @RequestBody CancelByPhoneRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest) {

        // Rate limiting по IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkLimit(clientIp);

        LunchBooking cancelled = lunchBookingService.cancelByCodeAndPhone(request.getCancelCode(), request.getPhoneNumber());
        return ResponseEntity.ok(LunchBookingResponse.from(cancelled));
    }

    @PostMapping("/booking/{id}/approve")
    @Operation(
            summary = "Подтверждение бронирования",
            description = """
                    Подтверждает бронирование, переводя его в статус APPROVED (одобрен/выполняется).
                    Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование подтверждено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование не в статусе PENDING")
    @PreAuthorize("hasAnyAuthority('ROLE_FRANCHISE_OWNER', 'ROLE_GLOBAL_ADMIN', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<LunchBookingResponse> approveBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable("id") java.util.UUID id) {
        LunchBooking approved = lunchBookingService.approveBooking(id);
        return ResponseEntity.ok(LunchBookingResponse.from(approved));
    }

    @PostMapping("/booking/{id}/complete")
    @Operation(
            summary = "Выполнение бронирования",
            description = """
                    Завершает бронирование, переводя его в статус COMPLETED (выполнен).
                    Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование выполнено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование не в статусе APPROVED")
    @PreAuthorize("hasAnyAuthority('ROLE_FRANCHISE_OWNER', 'ROLE_GLOBAL_ADMIN', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<LunchBookingResponse> completeBooking(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable("id") java.util.UUID id) {
        LunchBooking completed = lunchBookingService.completeBooking(id);
        return ResponseEntity.ok(LunchBookingResponse.from(completed));
    }

    @PostMapping("/booking/{id}/cancel")
    @Operation(
            summary = "Отмена бронирования админом",
            description = """
                    Отменяет бронирование из любого статуса (кроме CANCELLED и COMPLETED).
                    Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
                """,
            tags = {ApiTags.LUNCH_BOOKINGS, ApiTags.GLOBAL_ADMIN}
    )
    @ApiResponse(responseCode = "200", description = "Бронирование отменено")
    @ApiResponse(responseCode = "404", description = "Бронирование не найдено")
    @ApiResponse(responseCode = "409", description = "Бронирование уже отменено или выполнено")
    @PreAuthorize("hasAnyAuthority('ROLE_FRANCHISE_OWNER', 'ROLE_GLOBAL_ADMIN', 'ROLE_LOCATION_ADMIN')")
    public ResponseEntity<LunchBookingResponse> cancelByAdmin(
            @Parameter(description = "ID бронирования", required = true)
            @PathVariable("id") java.util.UUID id) {
        LunchBooking cancelled = lunchBookingService.cancelByAdmin(id);
        return ResponseEntity.ok(LunchBookingResponse.from(cancelled));
    }

    /**
     * Извлечь IP-адрес клиента из запроса (учитывает proxy).
     */
    private String getClientIp(HttpServletRequest request) {
        return IpAddressUtils.getClientIp(request);
    }
}
