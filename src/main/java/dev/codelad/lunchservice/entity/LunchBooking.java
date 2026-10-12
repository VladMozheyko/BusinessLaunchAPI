package dev.codelad.lunchservice.entity;

import dev.codelad.lunchservice.enums.BookingStatus;
import dev.codelad.lunchservice.util.CancelCodeGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Бронирование бизнес-ланча.
 * Контейнер, содержащий позиции заказа.
 */
@Entity
@Table(name = "lunch_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchBooking {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * ID пользователя, который создал бронирование (externalId из Auth-сервиса).
     */
    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private java.util.UUID userId;

    /**
     * Заведение, к которому привязано бронирование.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueProfile venue;

    /**
     * Время начала слота ланча (только время, без даты).
     */
    @Column(name = "time", nullable = false)
    private LocalTime time;

    /**
     * Статус бронирования.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * Уникальный код для отмены бронирования (генерируется автоматически).
     * Формат: A1B-2C3-D4E (3 символа-тире-3 символа-тире-3 символа).
     * Используется для отмены без авторизации.
     */
    @Column(name = "cancel_code", nullable = false, unique = true, length = 11)
    private String cancelCode;

    /**
     * Номер телефона пользователя для отмены бронирования.
     * Используется для идентификации незарегистрированных пользователей.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Общее количество гостей (сумма quantity всех позиций).
     * Пересчитывается автоматически.
     */
    @Column(name = "total_guests", nullable = false)
    @Builder.Default
    private Integer totalGuests = 0;

    /**
     * Позиции заказа.
     * Каскадное удаление: при удалении брони все позиции удаляются.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LunchOrderItem> items = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
        recalculateTotalGuests();
        if (cancelCode == null || cancelCode.isBlank()) {
            this.cancelCode = CancelCodeGenerator.generate();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        recalculateTotalGuests();
    }

    /**
     * Пересчитать totalGuests на основе позиций заказа.
     */
    @Transient
    protected void recalculateTotalGuests() {
        if (items != null && !items.isEmpty()) {
            this.totalGuests = items.stream()
                    .mapToInt(LunchOrderItem::getQuantity)
                    .sum();
        } else {
            this.totalGuests = 0;
        }
    }
}
