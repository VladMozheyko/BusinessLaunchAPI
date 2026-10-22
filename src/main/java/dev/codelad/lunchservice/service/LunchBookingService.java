package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.constant.Messages;
import dev.codelad.lunchservice.dto.request.CreateLunchBookingRequest;
import dev.codelad.lunchservice.entity.*;
import dev.codelad.lunchservice.enums.BookingStatus;
import dev.codelad.lunchservice.enums.NotificationEventType;
import dev.codelad.lunchservice.exception.BookingConflictException;
import dev.codelad.lunchservice.exception.ResourceNotFoundException;
import dev.codelad.lunchservice.exception.ValidationException;
import dev.codelad.lunchservice.listener.NotificationEventPublisher;
import dev.codelad.lunchservice.repository.DishRepository;
import dev.codelad.lunchservice.repository.LunchBookingRepository;
import dev.codelad.lunchservice.repository.UserRepository;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
import dev.codelad.lunchservice.repository.VenueScheduleRepository;
import dev.codelad.lunchservice.security.SecurityContextHelper;
import dev.codelad.lunchservice.util.VenueScheduleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LunchBookingService {

    private final LunchBookingRepository lunchBookingRepository;
    private final DishRepository dishRepository;
    private final VenueProfileRepository venueProfileRepository;
    private final VenueScheduleRepository venueScheduleRepository;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;

    @Transactional
    public LunchBooking createLunchBooking(CreateLunchBookingRequest request) {
        // 1. Проверка существования заведения
        var venue = validateVenue(request.getVenueId());

        // 2. Проверка рабочего времени заведения на выбранный день и время
        VenueScheduleUtils.checkVenueSchedule(venueScheduleRepository, venue.getId(), request.getTime());

        // 3. Валидация позиций и создание сущностей
        List<LunchOrderItem> items = new ArrayList<>();
        int totalGuests = 0;

        for (CreateLunchBookingRequest.OrderItemDto itemDto : request.getItems()) {
            LunchOrderItem item = processOrderItem(itemDto);
            items.add(item);
            totalGuests += itemDto.getQuantity();
        }

        // 4. Проверка общего количества гостей
        validateTotalGuests(totalGuests);

        // 5. Проверка занятости и вместимости
        validateCapacity(request.getVenueId(), request.getTime(), totalGuests, venue);

        // 6. Создание бронирования
        LunchBooking booking = buildBooking(request, venue, items);

        LunchBooking saved = lunchBookingRepository.save(booking);
        log.info("[LUNCH_BOOKING_CREATED] Бронирование {} создано: venue={}, time={}, guests={}",
                saved.getId(), request.getVenueId(), request.getTime(), totalGuests);

        // Отправляем push-уведомление пользователю
        sendBookingCreatedNotification(saved, venue.getName());

        return saved;
    }

    /**
     * Проверить существование заве��ения.
     */
    private VenueProfile validateVenue(Long venueId) {
        return venueProfileRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(Messages.VENUE_NOT_FOUND, venueId)));
    }

    /**
     * Обработать одну позицию заказа.
     */
    private LunchOrderItem processOrderItem(
            CreateLunchBookingRequest.OrderItemDto itemDto) {
        Dish dish = validateDish(itemDto.getDishId(), itemDto.getCategoryId());
        validateDishQuantity(dish, itemDto.getQuantity());
        validateItemQuantity(itemDto.getQuantity());

        return buildOrderItem(dish, itemDto);
    }

    /**
     * Проверить существование и принадлежность блюда категории.
     */
    private Dish validateDish(java.util.UUID dishId, Long categoryId) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(Messages.DISH_NOT_FOUND, dishId)));

        if (!dish.getCategoryId().equals(categoryId)) {
            throw new BookingConflictException(
                    String.format(Messages.DISH_NOT_BELONGS_TO_CATEGORY, dishId, categoryId));
        }

        if (!dish.isAvailable()) {
            throw new BookingConflictException(
                    String.format(Messages.DISH_UNAVAILABLE, dish.getName()));
        }

        return dish;
    }

    /**
     * Проверить лимит количества блюда.
     */
    private void validateDishQuantity(Dish dish, int requestedQuantity) {
        if (dish.getQuantity() != null && dish.getQuantity() >= 0) {
            int remaining = dish.getQuantity();
            if (remaining < requestedQuantity) {
                throw new BookingConflictException(
                        String.format(Messages.DISH_QUANTITY_LIMIT, remaining, dish.getName()));
            }
        }
    }

    /**
     * Проверить количество позиции.
     */
    private void validateItemQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(Messages.QUANTITY_MUST_BE_POSITIVE);
        }
    }

    /**
     * Построить позицию заказа.
     */
    private LunchOrderItem buildOrderItem(Dish dish, CreateLunchBookingRequest.OrderItemDto itemDto) {
        return LunchOrderItem.builder()
                .dish(dish)
                .quantity(itemDto.getQuantity())
                .priceAtOrder(dish.getPrice() != null ? BigDecimal.valueOf(dish.getPrice()) : BigDecimal.ZERO)
                .dishNameAtOrder(dish.getName())
                .dishWeightAtOrder(dish.getWeight())
                .dishWeightUnitAtOrder(dish.getWeightUnit())
                .build();
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
     * Проверить вместимость заведения.
     */
    private void validateCapacity(
            Long venueId, java.time.LocalTime time, int totalGuests, VenueProfile venue) {

        Integer currentOccupancy = lunchBookingRepository.sumTotalGuestsByVenueAndTime(venueId, time);
        Integer venueCapacity = venue.getCapacity();

        if (venueCapacity != null && venueCapacity > 0 &&
            currentOccupancy + totalGuests > venueCapacity) {
            throw new BookingConflictException(
                    String.format(Messages.VENUE_NO_CAPACITY, venueCapacity - currentOccupancy, venueCapacity));
        }
    }

    /**
     * Построить бронирование.
     */
    private LunchBooking buildBooking(
            CreateLunchBookingRequest request, VenueProfile venue, List<LunchOrderItem> items) {

        // Получаем текущий noShowCount для пользователя
        Integer currentNoShowCount = 0;
        String externalIdStr = securityContextHelper.getCurrentExternalId();
        var userOpt = userRepository.findByExternalId(externalIdStr);
        if (userOpt.isPresent()) {
            currentNoShowCount = userOpt.get().getNoShowCount() != null ? userOpt.get().getNoShowCount() : 0;
        }

        LunchBooking booking = LunchBooking.builder()
                .userId(externalIdStr)
                .venue(venue)
                .time(request.getTime())
                .status(BookingStatus.PENDING)
                .items(items)
                .noShowCount(currentNoShowCount)
                .build();

        for (LunchOrderItem item : items) {
            item.setBooking(booking);
        }

        return booking;
    }



    /**
     * Отменить бронирование по коду отмены.
     * Пользователь может отменить в любой момент, пока заказ не выполнен.
     * @param cancelCode код отмены
     * @return отменённое бронирование
     * @throws ResourceNotFoundException если бронирование не найдено
     * @throws BookingConflictException если бронирование уже отменено или выполнено
     */
    @Transactional
    public LunchBooking cancelByCode(String cancelCode) {
        var bookingOpt = lunchBookingRepository.findByCancelCode(cancelCode);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.CANCEL_BOOKING_NOT_FOUND, cancelCode));
        }

        LunchBooking booking = bookingOpt.get();
        LunchBooking cancelled = cancelBooking(booking);

        log.info("[LUNCH_BOOKING_CANCELLED] Бронирование {} отменено по коду: {}", cancelled.getId(), cancelCode);

        // Отправляем push-уведомление пользователю
        sendBookingCancelledNotification(cancelled, cancelled.getVenue().getName());

        return cancelled;
    }

    /**
     * Отменить бронирование по коду и номеру телефона.
     * Для незарегистрированных пользователей.
     * @param cancelCode код отмены
     * @param phoneNumber номер телефона
     * @return отменённое бронирование
     * @throws ResourceNotFoundException если бронирование не найдено или номер не совпадает
     * @throws BookingConflictException если бронирование уже отменено или выполнено
     */
    @Transactional
    public LunchBooking cancelByCodeAndPhone(String cancelCode, String phoneNumber) {
        var bookingOpt = lunchBookingRepository.findByCancelCode(cancelCode);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Бронирование с кодом отмены '" + cancelCode + "' не найдено");
        }

        LunchBooking booking = bookingOpt.get();

        // Проверка совпадения номера телефона
        if (booking.getPhoneNumber() == null || !booking.getPhoneNumber().equals(phoneNumber)) {
            throw new ResourceNotFoundException(
                    String.format(Messages.CANCEL_BOOKING_NOT_FOUND_OR_PHONE_MISMATCH, cancelCode));
        }

        LunchBooking cancelled = cancelBooking(booking);

        log.info("[LUNCH_BOOKING_CANCELLED] Бронирование {} отменено по коду и телефону: {}, phone={}",
                cancelled.getId(), cancelCode, phoneNumber);

        // Отправляем push-уведомление пользователю
        sendBookingCancelledNotification(cancelled, cancelled.getVenue().getName());

        return cancelled;
    }

    /**
     * Подтвердить бронирование (PENDING → APPROVED).
     * Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
     * @param bookingId ID бронирования
     * @return подтверждённое бронирование
     * @throws ResourceNotFoundException если бронирование не найдено
     * @throws BookingConflictException если бронирование не в статусе PENDING
     */
    @Transactional
    public LunchBooking approveBooking(java.util.UUID bookingId) {
        var bookingOpt = lunchBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        LunchBooking booking = bookingOpt.get();

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException(
                    String.format(Messages.APPROVE_PENDING_ONLY, booking.getStatus()));
        }

        booking.setStatus(BookingStatus.APPROVED);
        // Устанавливаем время истечения: booking.time + 40 минут
        java.time.LocalDateTime expiredAtDateTime = java.time.LocalDateTime.of(
                java.time.LocalDate.now(),
                booking.getTime()
        ).plusMinutes(40);
        booking.setExpiredAt(expiredAtDateTime.toInstant(
                java.time.ZoneOffset.systemDefault().getRules().getOffset(expiredAtDateTime)
        ));
        LunchBooking approved = lunchBookingRepository.save(booking);

        log.info("[LUNCH_BOOKING_APPROVED] Бронирование {} подтверждено: venue={}, time={}",
                approved.getId(), approved.getVenue().getId(), approved.getTime());

        // Отправляем push-уведомление пользователю
        sendBookingApprovedNotification(approved, approved.getVenue().getName());

        return approved;
    }

    /**
     * Выполнить бронирование (APPROVED → COMPLETED).
     * Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
     * @param bookingId ID бронирования
     * @return выполненное бронирование
     * @throws ResourceNotFoundException если бронирование не найдено
     * @throws BookingConflictException если бронирование не в статусе APPROVED
     */
    @Transactional
    public LunchBooking completeBooking(java.util.UUID bookingId) {
        var bookingOpt = lunchBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        LunchBooking booking = bookingOpt.get();

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BookingConflictException(
                    String.format(Messages.COMPLETE_APPROVED_ONLY, booking.getStatus()));
        }

        booking.setStatus(BookingStatus.COMPLETED);
        LunchBooking completed = lunchBookingRepository.save(booking);

        log.info("[LUNCH_BOOKING_COMPLETED] Бронирование {} выполнено: venue={}, time={}",
                completed.getId(), completed.getVenue().getId(), completed.getTime());

        // Отправляем push-уведомление пользователю
        sendBookingCompletedNotification(completed, completed.getVenue().getName());

        return completed;
    }

    /**
     * Отменить бронирование админом (PENDING или APPROVED → CANCELLED).
     * Только для FRANCHISE_OWNER, GLOBAL_ADMIN, LOCATION_ADMIN.
     * @param bookingId ID бронирования
     * @return отменённое бронирование
     * @throws ResourceNotFoundException если бронирование не найдено
     * @throws BookingConflictException если бронирование не в статусе PENDING или APPROVED
     */
    @Transactional
    public LunchBooking cancelByAdmin(java.util.UUID bookingId) {
        var bookingOpt = lunchBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        LunchBooking booking = bookingOpt.get();

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BookingConflictException(
                    String.format("Бронирование можно отменить только из статусов PENDING или APPROVED. Текущий статус: %s", booking.getStatus()));
        }

        LunchBooking cancelled = cancelBooking(booking);

        log.info("[LUNCH_BOOKING_CANCELLED_BY_ADMIN] Бронирование {} отменено админом: venue={}, time={}",
                cancelled.getId(), cancelled.getVenue().getId(), cancelled.getTime());

        // Отправляем push-уведомление пользователю
        sendBookingCancelledByAdminNotification(cancelled, cancelled.getVenue().getName());

        return cancelled;
    }

    /**
     * Получить все бронирования текущего пользователя.
     * @return список всех бронирований текущего пользователя
     */
    @Transactional(readOnly = true)
    public List<LunchBooking> getUserBookings() {
        String externalIdStr = securityContextHelper.getCurrentExternalId();
        List<LunchBooking> bookings = lunchBookingRepository.findByUserIdOrderByCreatedAtDesc(externalIdStr);
        log.info("[USER_BOOKINGS] Получены бронирования для пользователя {}: {} шт.",
                externalIdStr, bookings.size());
        return bookings;
    }

    /**
     * Получить бронирование по ID с проверкой прав доступа.
     * Для пользователя - только свои бронирования.
     * Для админа - любые бронирования.
     * @param bookingId ID бронирования
     * @return бронирование
     * @throws ResourceNotFoundException если бронирование не найдено
     */
    @Transactional(readOnly = true)
    public LunchBooking getBookingById(java.util.UUID bookingId) {
        var bookingOpt = lunchBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        LunchBooking booking = bookingOpt.get();

        // Проверка: пользователь может видеть только свои бронирования
        var currentUser = securityContextHelper.getCurrentUser();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.requireNonNull(a.getAuthority()).startsWith("ROLE_GLOBAL_ADMIN") ||
                               a.getAuthority().startsWith("ROLE_FRANCHISE_OWNER") ||
                               a.getAuthority().startsWith("ROLE_LOCATION_ADMIN"));

        if (!isAdmin && !booking.getUserId().equals(currentUser.getUserId())) {
            throw new BookingConflictException(Messages.ACCESS_DENIED);
        }

        return booking;
    }

    /**
     * Отменить бронирование: проверить статус и установить CANCELLED.
     * Бросает BookingConflictException если уже отменено или выполнено.
     */
    private LunchBooking cancelBooking(LunchBooking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingConflictException(
                    String.format(Messages.BOOKING_CANCELLED, booking.getId()));
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingConflictException(
                    String.format(Messages.BOOKING_COMPLETED, booking.getId()));
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return lunchBookingRepository.save(booking);
    }

    /**
     * Отметить бронирование как NO_SHOW (неявка).
     * Только для администраторов.
     * @param bookingId ID бронирования
     * @return отмеченное бронирование
     */
    @Transactional
    public LunchBooking markNoShow(java.util.UUID bookingId) {
        var bookingOpt = lunchBookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(Messages.BOOKING_NOT_FOUND, bookingId));
        }

        LunchBooking booking = bookingOpt.get();

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingConflictException(
                    String.format("Нельзя отметить как NO_SHOW бронирование со статусом %s", booking.getStatus()));
        }

        booking.setStatus(BookingStatus.NO_SHOW);

        // Increment noShowCount для пользователя
        if (booking.getUserId() != null) {
            var userOpt = userRepository.findByExternalId(booking.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setNoShowCount((user.getNoShowCount() != null ? user.getNoShowCount() : 0) + 1);
                userRepository.save(user);
            }
        }

        LunchBooking marked = lunchBookingRepository.save(booking);
        log.info("[BOOKING_NO_SHOW] Бронирование {} отмечено как неявка", bookingId);

        return marked;
    }

    // ==================== Методы для отправки уведомлений ====================

    /**
     * Отправляет уведомление о создании брони.
     */
    private void sendBookingCreatedNotification(LunchBooking booking, String venueName) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("venue", venueName);
            templateData.put("bookingId", booking.getId().toString());
            templateData.put("time", booking.getTime().toString());
            templateData.put("guests", String.valueOf(booking.getTotalGuests()));

            notificationEventPublisher.publishLunchBookingEvent(
                    userId,
                    NotificationEventType.BOOKING_CREATED,
                    "Бронь создана",
                    "Ваша бронь в " + venueName + " создана. Время: " + booking.getTime() + ", гостей: " + booking.getTotalGuests(),
                    templateData,
                    null // JWT токен не нужен для публичного endpoint
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send booking created notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об отмене брони.
     */
    private void sendBookingCancelledNotification(LunchBooking booking, String venueName) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("venue", venueName);
            templateData.put("bookingId", booking.getId().toString());

            notificationEventPublisher.publishLunchBookingEvent(
                    userId,
                    NotificationEventType.BOOKING_CANCELLED,
                    "Бронь отменена",
                    "Ваша бронь в " + venueName + " отменена.",
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send booking cancelled notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об отмене брони администратором.
     */
    private void sendBookingCancelledByAdminNotification(LunchBooking booking, String venueName) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("venue", venueName);
            templateData.put("bookingId", booking.getId().toString());

            notificationEventPublisher.publishLunchBookingEvent(
                    userId,
                    NotificationEventType.BOOKING_CANCELLED_BY_ADMIN,
                    "Бронь отменена админом",
                    "Ваша бронь в " + venueName + " отменена администратором.",
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send booking cancelled by admin notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление об одобрении брони.
     */
    private void sendBookingApprovedNotification(LunchBooking booking, String venueName) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("venue", venueName);
            templateData.put("bookingId", booking.getId().toString());
            templateData.put("time", booking.getTime().toString());

            notificationEventPublisher.publishLunchBookingEvent(
                    userId,
                    NotificationEventType.BOOKING_APPROVED,
                    "Бронь одобрена",
                    "Ваша бронь в " + venueName + " одобрена. Время: " + booking.getTime(),
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send booking approved notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }

    /**
     * Отправляет уведомление о завершении брони.
     */
    private void sendBookingCompletedNotification(LunchBooking booking, String venueName) {
        try {
            String userId = securityContextHelper.getCurrentExternalId().toString();
            
            Map<String, String> templateData = new HashMap<>();
            templateData.put("venue", venueName);
            templateData.put("bookingId", booking.getId().toString());

            notificationEventPublisher.publishLunchBookingEvent(
                    userId,
                    NotificationEventType.BOOKING_COMPLETED,
                    "Бронь завершена",
                    "Ваша бронь в " + venueName + " завершена. Спасибо, что пришли!",
                    templateData,
                    null
            );
        } catch (Exception e) {
            log.error("[NOTIFICATION_ERROR] Failed to send booking completed notification: bookingId={}, error={}",
                    booking.getId(), e.getMessage(), e);
        }
    }
}
