package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Статус последней синхронизации меню для заведения.
 * Хранит информацию о том, когда была последняя синхронизация
 * и сколько категорий/блюд было синхронизировано.
 */
@Entity
@Table(name = "food_sync_status")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FoodSyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "venue_id", nullable = false, unique = true)
    private Long venueId;

    /**
     * Время последней успешной синхронизации.
     */
    @Column(name = "last_sync_time")
    private Instant lastSyncTime;

    /**
     * Количество синхронизированных категорий.
     */
    @Column(name = "synced_categories_count")
    private Integer syncedCategoriesCount;

    /**
     * Количество синхронизированных блюд.
     */
    @Column(name = "synced_dishes_count")
    private Integer syncedDishesCount;

    /**
     * Тип синхронизации: SCHEDULED (планируемая), MANUAL (ручная), LAZY (ленивая).
     */
    @Column(name = "sync_type", length = 20)
    @Builder.Default
    private String syncType = "SCHEDULED";

    /**
     * Сообщение об ошибке, если синхронизация завершилась неудачно.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (lastSyncTime == null) {
            lastSyncTime = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
    }
}
