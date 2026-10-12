package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /**
     * Категория, которой принадлежит блюдо.
     * При удалении категории блюдо удаляется автоматически (orphanRemoval).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
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
