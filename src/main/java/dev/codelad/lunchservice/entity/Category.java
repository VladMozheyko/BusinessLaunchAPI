package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /**
     * Карточка заведения, к которой привязана категория.
     * Один VenueProfile может иметь несколько категорий.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_profile_id", nullable = false, referencedColumnName = "id")
    private VenueProfile venueProfile;

    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Скрыта ли категория (мягкое удаление).
     */
    @Column(name = "hidden")
    private Boolean hidden = false;

    /**
     * Синхронизирована ли категория из Food Service.
     * true — данные подтянуты из кассового аппарата (Food Service).
     * false — локальная категория, созданная пользователем.
     */
    @Column(name = "synced_from_food")
    @Builder.Default
    private Boolean syncedFromFood = false;

    /**
     * Блюда, принадлежащие этой категории.
     * Каскадное удаление: при удалении категории все связанные блюда удаляются автоматически.
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dish> dishes = new ArrayList<>();

    /**
     * Время создания категории (UTC).
     */
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Время последнего обновления (UTC).
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
