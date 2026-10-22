package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.constant.Messages;
import dev.codelad.lunchservice.dto.request.CreateComboBookingRequest;
import dev.codelad.lunchservice.entity.*;
import dev.codelad.lunchservice.enums.ComboBookingStatus;
import dev.codelad.lunchservice.enums.NotificationEventType;
import dev.codelad.lunchservice.exception.BookingConflictException;
import dev.codelad.lunchservice.exception.ResourceNotFoundException;
import dev.codelad.lunchservice.exception.ValidationException;
import dev.codelad.lunchservice.listener.NotificationEventPublisher;
import dev.codelad.lunchservice.repository.ComboBookingRepository;
import dev.codelad.lunchservice.repository.ComboRepository;
import dev.codelad.lunchservice.repository.DishRepository;
import dev.codelad.lunchservice.repository.UserRepository;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
import dev.codelad.lunchservice.repository.VenueScheduleRepository;
import dev.codelad.lunchservice.security.SecurityContextHelper;
import dev.codelad.lunchservice.util.VenueScheduleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для управления бронированиями комбо-наборов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComboBookingService {

    private final ComboBookingRepository comboBookingRepository;
    private final ComboRepository comboRepository;
    private final DishRepository dishRepository;
    private final VenueProfileRepository venueProfileRepository;
    private final VenueScheduleRepository venueScheduleRepository;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;

    /**
     * Создать бронирование комбо-набора.
     */
    @Transactional
    public ComboBooking createComboBooking(CreateComboBookingRequest request) {
        // 1. Проверка существования заведения
        var venue = validateVenue(request.getVenueId());

        // 2. Проверка рабочего времени заведения
        VenueScheduleUtils.checkVenueSchedule(venueScheduleRepository, venue.getId(), request.getTime());

        // 3. Валидация позиций комбо и создание сущностей
        List<ComboBookingItem> items = new ArrayList<>();
        int totalGuests = 0;

        for (CreateComboBookingRequest.ComboItemRequest itemRequest : request.getItems()) {
            ComboBookingItem item = processComboItem(itemRequest, request.getVenueId());
            items.add(item);
            totalGuests += itemRequest.getQuantity();
        }

        // 4. Проверка общего количества гостей
        validateTotalGuests(totalGuests);

        // 5. Создание бронирования
        ComboBooking booking = buildBooking(request, items);

        ComboBooking saved = comboBookingRepository.save(booking);
        log.info("[COMBO_BOOKING_CREATED] Бронирование {} создано: venue={}, time={}, guests={}",
                saved.getId(), request.getVenueId(), request.getTime(), totalGuests);

        // Отправляем push-уведомление пользователю
        sendComboBookingCreatedNotification(saved, request.getVenueId());

        return saved;
    }

    /**
     * Проверить существование заведения.
     */
    private VenueProfile validateVenue(Long venueId) {
        return venueProfileRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(Messages.VENUE_NOT_FOUND, venueId)));
    }

    /**
     * Обработать одну позицию комбо.
     */
    private ComboBookingItem processComboItem(
            CreateComboBookingRequest.ComboItemRequest itemRequest, Long venueId) {

        Combo combo = validateCombo(itemRequest.getComboId(), venueId);
        validateComboDailyLimit(combo.getId(), itemRequest.getQuantity());

        ComboBookingItem item = buildComboItem(combo, itemRequest);

        if (itemRequest.getAddons() != null && !itemRequest.getAddons().isEmpty()) {
            processAddons(item, combo, itemRequest.getAddons());
        }

        return item;
    }

    /**
     * Проверить существование и принадлежность комбо заведению.
     */
    private Combo validateCombo(Long comboId, Long venueId) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(Messages.COMBO_NOT_FOUND, comboId)));

        if (!combo.getVenueId().equals(venueId)) {
            throw new BookingConflictException(
                    String.format(Messages.COMBO_NOT_BELONGS_TO_VENUE, comboId, venueId));
        }

        if (!combo.isActive()) {
            throw new BookingConflictException(
                    String.format(Messages.COMBO_UNAVAILABLE, combo.getName()));
        }

        return combo;
    }

    /**
     * Построить позицию заказа комбо.
     */
    private ComboBookingItem buildComboItem(
            Combo combo, CreateComboBookingRequest.ComboItemRequest itemRequest) {
        return ComboBookingItem.builder()
                .comboId(combo.getId())
                .comboName(combo.getName())
                .quantity(itemRequest.getQuantity())
                .priceAtOrder(combo.getPrice())
                .build();
    }

    /**
     * Обработать дополнения к комбо.
     */
    private void processAddons(
            ComboBookingItem item, Combo combo, List<CreateComboBookingRequest.ComboAddonRequest> addons) {
        for (CreateComboBookingRequest.ComboAddonRequest addonRequest : addons) {
            Dish dish = validateAddonDish(addonRequest, combo);

            ComboAddon addon = ComboAddon.builder()
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .quantity(addonRequest.getQuantity())
                    .priceAtOrder(dish.getPrice() != null ? dish.getPrice() : 0)
                    .build();

            item.getAddons().add(addon);
        }
    }

    /**
     * Проверить существование и валидность блюда-дополнения.
     */
    private Dish validateAddonDish(
            CreateComboBookingRequest.ComboAddonRequest addonRequest, Combo combo) {

        Dish dish = dishRepository.findById(addonRequest.getDishId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(Messages.DISH_NOT_FOUND, addonRequest.getDishId())));

        if (combo.getAddons().stream().noneMatch(a -> a.getId().equals(dish.getId()))) {
            throw new BookingConflictException(
                    String.format(Messages.ADDON_NOT_VALID, dish.getName(), combo.getName()));
        }

        if (!dish.isAvailable()) {
            throw new BookingConflictException(
                    String.format(Messages.DISH_UNAVAILABLE, dish.getName()));
        }

        return dish;
    }

    /**
     * Проверить общее количество гостей.
     */
    private void validateTotalGuests(int totalGuests) {
        if (totalGuests <= 0) {
            throw new BookingConflictException(Messages.TOTAL_GUESTS_MUST_BE_POSITIVE);
        }
    }

    /**
     * Построить бронирование.
     */
    private ComboBooking buildBooking(
            CreateComboBookingRequest request, List<ComboBookingItem> items) {
        ComboBooking booking = ComboBooking.builder()
                .venueId(request.getVenueId())
                .time(request.getTime())
                .status(ComboBookingStatus.PENDING)
                .phoneNumber(request.getPhoneNumber())
                .customerName(request.getCustomerName())
                .comment(request.getComment())
                .items(items)
                .build();

        for (ComboBookingItem item : items) {
            item.setComboBooking(booking);
        }

        return booking;
    }

    /**
     * Проверить лимит комбо в день.
     */
    private void validateComboDailyLimit(Long comboId, int requestedQuantity) {
        var comboOpt = comboRepository.findById(comboId);
        if (comboOpt.isEmpty()) {
            return;
        }

        Combo combo = comboOpt.get();
        Integer dailyLimit = combo.getDailyLimit();

        if (dailyLimit == null || dailyLimit == 0) {
            return; // Без лимита
        }

        // Подсчитать текущие активные заказы на сегодня
        List<ComboBookingStatus> activeStatuses = List.of(ComboBookingStatus.PENDING, ComboBookingStatus.APPROVED);
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        int currentOrders = comboBookingRepository.sumTotalGuestsByComboIdAndDate(
                comboId, activeStatuses, today, today.plusDays(1));

        if (currentOrders + requestedQuantity > dailyLimit) {
            throw new BookingConflictException(
                    String.format(Messages.COMBO_DAILY_LIMIT_EXCEEDED, combo.getName(),
                            dailyLimit - currentOrders, dailyLimit));
        }
    }

    /**
     * Отменить бронирование по коду отмены.
     */
    @Transactional
    public ComboBooking cancelByCode(String cancelCode) {
        var bookingOpt = comboBookingRepository.findByCancelCode(cancelCode);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.CANCEL_BOOKING_NOT_FOUND, cancelCode));
        }

        ComboBooking booking = bookingOpt.get();

        if (booking.getStatus() == ComboBookingStatus.CANCELLED) {
            throw new BookingConflictException(
                    String.format(Messages.BOOKING_CANCELLED, booking.getId()));
        }

        if (booking.getStatus() == ComboBookingStatus.COMPLETED) {
            throw new BookingConflictException(
                    String.format(Messages.BOOKING_COMPLETED, booking.getId()));
        }

        booking.setStatus(ComboBookingStatus.CANCELLED);
        ComboBooking cancelled = comboBookingRepository.save(booking);

        log.info("[COMBO_BOOKING_CANCELLED] Бронирование {} отменено по коду: {}", cancelled.getId(), cancelCode);

        // Отправляем push-уведомление пользователю
        sendComboBookingCancelledNotification(cancelled);

        return cancelled;
    }

    /**
     * Отменить бронирование по коду и номеру телефона.
     * Для незарегистрированных пользователей (гостей).
     */
    @Transactional
    public ComboBooking cancelByCodeAndPhone(String cancelCode, String phoneNumber) {
        var bookingOpt = comboBookingRepository.findByCancelCode(cancelCode);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.CANCEL_BOOKING_NOT_FOUND, cancelCode));
        }

        ComboBooking booking = bookingOpt.get();

        // Проверка совпадения номера телефона
        if (booking.getPhoneNumber() == null || !booking.getPhoneNumber().equals(phoneNumber)) {
            throw new ResourceNotFoundException(
                    String.format(Messages.CANCEL_BOOKING_NOT_FOUND_OR_PHONE_MISMATCH, cancelCode));
        }

        if (booking.getStatus() == ComboBookingStatus.CANCELLED) {
            throw new BookingConflictException(
                    String.format(Messages.BOOKING_CANCELLED, booking.getId()));
        }

        if (booking.getStatus() == ComboBookingStatus.COMPLETED) {
            throw new BookingConflictException(
                    String.format(Messages.BOOKING_COMPLETED, booking.getId()));
        }

        booking.setStatus(ComboBookingStatus.CANCELLED);
        ComboBooking cancelled = comboBookingRepository.save(booking);

        log.info("[COMBO_BOOKING_CANCELLED] Бронирование {} отменено по коду и телефону: {}, phone={}",
                cancelled.getId(), cancelCode, phoneNumber);

        // Отправляем push-уведомление пользователю
        sendComboBookingCancelledNotification(cancelled);

        return cancelled;
    }

    /**
     * Получить все бронирования по номеру телефона.
     */
    @Transactional(readOnly = true)
    public List<ComboBooking> getBookingsByPhone(String phoneNumber) {
        List<ComboBooking> bookings = comboBookingRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
        log.info("[COMBO_BOOKINGS_BY_PHONE] Получены бронирования для телефона {}: {} шт.",
                phoneNumber, bookings.size());
        return bookings;
    }

    /**
     * Подтвердить бронирование (PENDING → APPROVED).
     */
    @Transactional
    public ComboBooking approveBooking(java.util.UUID bookingId) {
        var bookingOpt = comboBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        ComboBooking booking = bookingOpt.get();

        if (booking.getStatus() != ComboBookingStatus.PENDING) {
            throw new BookingConflictException(
                    String.format(Messages.APPROVE_PENDING_ONLY, booking.getStatus()));
        }

        booking.setStatus(ComboBookingStatus.APPROVED);
        // Устанавливаем время истечения: booking.time + 40 минут
        java.time.LocalDateTime expiredAtDateTime = java.time.LocalDateTime.of(
                java.time.LocalDate.now(),
                booking.getTime()
        ).plusMinutes(40);
        booking.setExpiredAt(expiredAtDateTime.toInstant(
                java.time.ZoneOffset.systemDefault().getRules().getOffset(expiredAtDateTime)
        ));
        ComboBooking approved = comboBookingRepository.save(booking);

        log.info("[COMBO_BOOKING_APPROVED] Бронирование {} подтверждено: venue={}, time={}",
                approved.getId(), approved.getVenueId(), approved.getTime());

        // Отправляем push-уведомление пользователю
        sendComboBookingApprovedNotification(approved);

        return approved;
    }

    /**
     * Выполнить бронирование (APPROVED → COMPLETED).
     */
    @Transactional
    public ComboBooking completeBooking(java.util.UUID bookingId) {
        var bookingOpt = comboBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        ComboBooking booking = bookingOpt.get();

        if (booking.getStatus() != ComboBookingStatus.APPROVED) {
            throw new BookingConflictException(
                    String.format(Messages.COMPLETE_APPROVED_ONLY, booking.getStatus()));
        }

        booking.setStatus(ComboBookingStatus.COMPLETED);
        ComboBooking completed = comboBookingRepository.save(booking);

        log.info("[COMBO_BOOKING_COMPLETED] Бронирование {} выполнено: venue={}, time={}",
                completed.getId(), completed.getVenueId(), completed.getTime());

        // Отправляем push-уведомление пользователю
        sendComboBookingCompletedNotification(completed);

        return completed;
    }

    /**
     * Отменить бронирование админом (PENDING или APPROVED → CANCELLED).
     */
    @Transactional
    public ComboBooking cancelByAdmin(java.util.UUID bookingId) {
        var bookingOpt = comboBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        ComboBooking booking = bookingOpt.get();

        if (booking.getStatus() != ComboBookingStatus.PENDING && booking.getStatus() != ComboBookingStatus.APPROVED) {
            throw new BookingConflictException(
                    String.format("Бронирование можно отменить только из статусов PENDING или APPROVED. Текущий статус: %s", booking.getStatus()));
        }

        booking.setStatus(ComboBookingStatus.CANCELLED);
        ComboBooking cancelled = comboBookingRepository.save(booking);

        log.info("[COMBO_BOOKING_CANCELLED_BY_ADMIN] Бронирование {} отменено админом: venue={}, time={}",
                cancelled.getId(), cancelled.getVenueId(), cancelled.getTime());

        // Отправляем push-уведомление пользователю
        sendComboBookingCancelledByAdminNotification(cancelled);

        return cancelled;
    }

    /**
     * Получить все бронирования текущего пользователя.
     * @return список всех бронирований текущего пользователя
     */
    @Transactional(readOnly = true)
    public List<ComboBooking> getUserBookings() {
        String externalIdStr = securityContextHelper.getCurrentExternalId();
        List<ComboBooking> bookings = comboBookingRepository.findByUserIdOrderByCreatedAtDesc(externalIdStr);
        log.info("[COMBO_USER_BOOKINGS] Получены бронирования для пользователя {}: {} шт.",
                externalIdStr, bookings.size());
        return bookings;
    }

    /**
     * Отметить бронирование как NO_SHOW (неявка).
     * Только для администраторов.
     * @param bookingId ID бронирования
     * @return отмеченное бронирование
     */
    @Transactional
    public ComboBooking markNoShow(java.util.UUID bookingId) {
        var bookingOpt = comboBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        ComboBooking booking = bookingOpt.get();

        if (booking.getStatus() == ComboBookingStatus.COMPLETED || booking.getStatus() == ComboBookingStatus.CANCELLED) {
            throw new BookingConflictException(
                    String.format("Нельзя отметить как NO_SHOW бронирование со статусом %s", booking.getStatus()));
        }

        booking.setStatus(ComboBookingStatus.NO_SHOW);

        // Increment noShowCount для пользователя
        if (booking.getUserId() != null) {
            var userOpt = userRepository.findByExternalId(booking.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setNoShowCount((user.getNoShowCount() != null ? user.getNoShowCount() : 0) + 1);
                userRepository.save(user);
            }
        }

        ComboBooking marked = comboBookingRepository.save(booking);
        log.info("[COMBO_BOOKING_NO_SHOW] Бронирование {} отмечено как неявка", bookingId);

        return marked;
    }

    // ==================== Методы для отправки уведомлений ====================

    /**
     * Отправляет уведомление о создании комбо-брони.
     */
    private void sendComboBookingCreatedNotification(ComboBooking booking, Long venueId) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId().toString());
            templateData.put("time", booking.getTime().toString());
            templateData.put("guests", String.valueOf(booking.getTotalGuests()));

            notificationEventPublisher.publishComboBookingEvent(
                    userId,
                    NotificationEventType.COMBO_BOOKING_CREATED,
                    "Комбо-бронь создана",
                    "Ваша бронь комбо-набора создана. Время: " + booking.getTime() + ", гостей: " + booking.getTotalGuests(),
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send combo booking created notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об отмене комбо-брони.
     */
    private void sendComboBookingCancelledNotification(ComboBooking booking) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId().toString());

            notificationEventPublisher.publishComboBookingEvent(
                    userId,
                    NotificationEventType.COMBO_BOOKING_CANCELLED,
                    "Комбо-бронь отменена",
                    "Ваша бронь комбо-набора отменена.",
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send combo booking cancelled notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об отмене комбо-брони администратором.
     */
    private void sendComboBookingCancelledByAdminNotification(ComboBooking booking) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId().toString());

            notificationEventPublisher.publishComboBookingEvent(
                    userId,
                    NotificationEventType.COMBO_BOOKING_CANCELLED_BY_ADMIN,
                    "Комбо-бронь отменена админом",
                    "Ваша бронь комбо-набора отменена администратором.",
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send combo booking cancelled by admin notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об одобрении комбо-брони.
     */
    private void sendComboBookingApprovedNotification(ComboBooking booking) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId().toString());
            templateData.put("time", booking.getTime().toString());

            notificationEventPublisher.publishComboBookingEvent(
                    userId,
                    NotificationEventType.COMBO_BOOKING_APPROVED,
                    "Комбо-бронь одобрена",
                    "Ваша бронь комбо-набора одобрена. Время: " + booking.getTime(),
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send combo booking approved notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление о завершении комбо-брони.
     */
    private void sendComboBookingCompletedNotification(ComboBooking booking) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("bookingId", booking.getId().toString());

            notificationEventPublisher.publishComboBookingEvent(
                    userId,
                    NotificationEventType.COMBO_BOOKING_COMPLETED,
                    "Комбо-бронь завершена",
                    "Ваша бронь комбо-набора завершена. Спасибо, что пришли!",
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send combo booking completed notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }
}
