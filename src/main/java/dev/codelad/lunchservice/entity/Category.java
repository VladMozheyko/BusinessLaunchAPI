package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(nullable = false, length = 255)
    private String name;

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
