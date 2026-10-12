package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Позиция заказа бизнес-ланча.
 */
@Entity
@Table(name = "lunch_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchOrderItem {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Бронирование, которому принадлежит позиция.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private LunchBooking booking;

    /**
     * Ссылка на ланч (сущность Dish).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    /**
     * Количество порций этого ланча.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Снимок данных блюда на момент заказа.
     */
    @Column(name = "dish_name_at_order", nullable = false, length = 200)
    private String dishNameAtOrder;

    @Column(name = "dish_weight_at_order")
    private Integer dishWeightAtOrder;

    @Column(name = "dish_weight_unit_at_order", length = 5)
    private String dishWeightUnitAtOrder;

    /**
     * Цена на момент заказа (фиксация цены).
     */
    @Column(name = "price_at_order", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtOrder;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
