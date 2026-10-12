package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Позиция заказа комбо-набора.
 */
@Entity
@Table(name = "combo_booking_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboBookingItem {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Бронирование, которому принадлежит позиция.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_booking_id", nullable = false)
    private ComboBooking comboBooking;

    /**
     * ID комбо.
     */
    @Column(name = "combo_id", nullable = false)
    private Long comboId;

    /**
     * Имя комбо (снимок при заказе).
     */
    @Column(name = "combo_name", nullable = false)
    private String comboName;

    /**
     * Количество комбо.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Цена комбо при заказе (в копейках).
     */
    @Column(name = "price_at_order", nullable = false)
    private Integer priceAtOrder;

    /**
     * Дополнения к позиции комбо.
     * Каскадное удаление.
     */
    @OneToMany(mappedBy = "comboBookingItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComboAddon> addons = new ArrayList<>();
}
