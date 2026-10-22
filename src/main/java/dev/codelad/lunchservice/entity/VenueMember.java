package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Связь пользователя с заведением (для LOCATION_MANAGER).
 * Хранит привязку пользователя к venueId с ролью MANAGER.
 * 
 * Пример:
 * - user_id = UUID пользователя (LOCATION_MANAGER)
 * - venue_id = Long ID заведения из Bar Auth (bar_id/brew_id)
 * - role_type = "MANAGER"
 * 
 * Один пользователь может быть менеджером нескольких заведений.
 */
@Entity
@Table(
    name = "venue_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"venue_id", "user_id"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /**
     * ID заведения (из Bar Auth: bar_id или brew_id).
     */
    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    /**
     * ID пользователя (externalId из JWT).
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Роль пользователя в заведении.
     * Сейчас только "MANAGER".
     */
    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType;

    /**
     * ID пользователя, который назначил менеджера.
     */
    @Column(name = "assigned_by")
    private UUID assignedBy;

    /**
     * Дата назначения.
     */
    @Column(name = "assigned_at")
    private Instant assignedAt;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
