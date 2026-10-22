package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Карточка заведения (Venue Profile).
 * Главная сущность, к которой привязываются все данные.
 * 
 * Создаётся автоматически при первом входе BAR_OWNER/BREW_OWNER:
 * - venueId — из Bar Auth JWT (bar_id/brew_id)
 * - ownerUserId — из Bar Auth JWT (user_id)
 * - name, description, logoUrl — заполняет пользователь позже
 */
@Entity
@Table(name = "venue_profiles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /**
     * ID заведения из Bar Auth (bar_id или brew_id).
     * Уникальный идентификатор заведения в Bar Auth системе.
     */
    @Column(name = "venue_id", nullable = false, unique = true)
    private Long venueId;

    /**
     * externalId владельца (из JWT — userUuid или user_id как String).
     */
    @Column(name = "external_id")
    private String externalId;

    /**
     * ID пользователя-владельца заведения (для обратной совместимости).
     */
    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    /**
     * Название заведения (заполняет пользователь).
     */
    @Column(name = "name")
    private String name;

    /**
     * Описание заведения (заполняет пользователь).
     */
    @Column(length = 2000)
    private String description;

    /**
     * URL логотипа заведения (заполняет пользователь).
     */
    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * Вместимость заведения (заполняет пользователь).
     */
    @Column(name = "capacity")
    private Integer capacity;

    /**
     * Бронирования этого заведения.
     */
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LunchBooking> bookings = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
