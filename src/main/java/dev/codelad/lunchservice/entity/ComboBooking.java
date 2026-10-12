package dev.codelad.lunchservice.entity;

import dev.codelad.lunchservice.enums.ComboBookingStatus;
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
 * Бронирование комбо-наборов бизнес-ланча.
 * Контейнер, содержащий позиции заказа с дополнениями.
 */
@Entity
@Table(name = "combo_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboBooking {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * ID пользователя (externalId из Auth-сервиса), который создал бронирование.
     */
    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.BINARY)
    private java.util.UUID userId;

    /**
     * ID заведения, к которому привязано бронирование.
     */
    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    /**
     * Время начала слота ланча (только время, без даты).
     */
    @Column(nullable = false)
    private LocalTime time;

    /**
     * Статус бронирования.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ComboBookingStatus status = ComboBookingStatus.PENDING;

    /**
     * Уникальный код для отмены бронирования.
     * Формат: XXX-XXX-XXX (3 символа-тире-3 символа-тире-3 символа).
     */
    @Column(name = "cancel_code", nullable = false, unique = true, length = 11)
    private String cancelCode;

    /**
     * Номер телефона для отмены бронирования.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Имя клиента.
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Комментарий к заказу.
     */
    @Column
    private String comment;

    /**
     * Общее количество гостей (сумма quantity всех позиций).
     */
    @Column(name = "total_guests", nullable = false)
    @Builder.Default
    private Integer totalGuests = 0;

    /**
     * Позиции заказа комбо.
     * Каскадное удаление: при удалении брони все позиции и дополнения удаляются.
     */
    @OneToMany(mappedBy = "comboBooking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComboBookingItem> items = new ArrayList<>();

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
                    .mapToInt(ComboBookingItem::getQuantity)
                    .sum();
        } else {
            this.totalGuests = 0;
        }
    }


}
