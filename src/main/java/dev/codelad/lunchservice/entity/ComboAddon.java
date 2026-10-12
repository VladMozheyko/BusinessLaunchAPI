package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Дополнение к позиции комбо-набора (конкретное блюдо).
 */
@Entity
@Table(name = "combo_addons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboAddon {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Позиция заказа, которой принадлежит дополнение.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_booking_item_id", nullable = false)
    private ComboBookingItem comboBookingItem;

    /**
     * ID блюда-дополнения.
     */
    @Column(name = "dish_id", nullable = false)
    private UUID dishId;

    /**
     * Имя блюда (снимок при заказе).
     */
    @Column(name = "dish_name", nullable = false)
    private String dishName;

    /**
     * Количество дополнений.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Цена дополнения при заказе.
     */
    @Column(name = "price_at_order", nullable = false)
    private Integer priceAtOrder;
}
