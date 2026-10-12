package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combos")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(nullable = false)
    private String name;

    /**
     * Цена комбо в копейках.
     */
    @Column(nullable = false)
    private Integer price;

    /**
     * Быстрый вкл/выкл.
     */
    @Column(name = "is_active")
    private boolean isActive;

    /**
     * Лимит ланчей в день (0 = без лимита).
     */
    @Column(name = "daily_limit")
    private Integer dailyLimit;

    /**
     * Категории, включённые в комбо.
     * Пользователь выбирает по одному блюду из каждой категории.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "combo_categories",
            joinColumns = @JoinColumn(name = "combo_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    /**
     * Блюда-дополнения, которые можно добавить к комбо.
     * Пользователь выбирает конкретные блюда и количество.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "combo_addon_dishes",
            joinColumns = @JoinColumn(name = "combo_id"),
            inverseJoinColumns = @JoinColumn(name = "dish_id")
    )
    @Builder.Default
    private List<Dish> addons = new ArrayList<>();

    /**
     * Блюда, которые входят в комбо бесплатно (дополнения без доплаты).
     * Например: хлеб, компот, салат.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "combo_free_addon_dishes",
            joinColumns = @JoinColumn(name = "combo_id"),
            inverseJoinColumns = @JoinColumn(name = "dish_id")
    )
    @Builder.Default
    private List<Dish> freeAddons = new ArrayList<>();

    /**
     * Время создания комбо (UTC).
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Время последнего обновления комбо (UTC).
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
