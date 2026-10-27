package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import dev.codelad.lunchservice.enums.DishType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dishes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Категория, которой принадлежит блюдо.
     * При удалении категории блюдо удаляется автоматически (orphanRemoval).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    /**
     * Вес или объём порции (число).
     */
    @Column(nullable = false)
    private Integer weight;

    /**
     * Единица измерения: "г" или "мл".
     */
    @Column(name = "weight_unit", nullable = false, length = 5)
    private String weightUnit;

    /**
     * Цена в копейках (если продаётся отдельно вне ланча).
     */
    private Integer price;

    /**
     * В наличии / нет.
     */
    @Column(name = "is_available")
    private boolean isAvailable;

    /**
     * Количество доступных блюд (необязательное).
     * null — не ограничено.
     * 0 — ни одного (недоступно).
     * >0 — ограниченное количество.
     */
    @Column(name = "quantity")
    private Integer quantity;

    /**
     * Бесплатное дополнение (хлеб, компот — входит в ланч без доплаты).
     */
    @Column(name = "is_free_addon")
    private boolean isFreeAddon;

    /**
     * Активно ли блюдо (мягкое удаление).
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Скрыто ли блюдо (не отображается в списке, но не удаляется).
     */
    @Column(name = "hidden")
    @Builder.Default
    private Boolean hidden = false;

    /**
     * Синхронизировано ли блюдо из Food Service.
     * true — данные подтянуты из кассового аппарата (Food Service).
     * false — локальное блюдо, созданное пользователем.
     */
    @Column(name = "synced_from_food")
    @Builder.Default
    private Boolean syncedFromFood = false;

    /**
     * Тип блюда: MAIN (основное) или ADDON (дополнение).
     */
    @Column(name = "dish_type", length = 10, nullable = false)
    @Builder.Default
    private DishType dishType = DishType.MAIN;

    /**
     * Время создания блюда (UTC).
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Время последнего обновления блюда (UTC).
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
