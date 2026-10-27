package dev.codelad.lunchservice.entity;

import dev.codelad.lunchservice.security.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность пользователя.
 * externalId — это userUuid из JWT-токена Auth-сервиса или user_id из Bar Auth-сервиса.
 * Роль хранится в БД, а не в JWT.
 */
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * userUuid из Base Auth или user_id из Bar Auth — уникальный идентификатор из auth-сервиса.
     * Хранится как String для поддержки UUID (550e8400-...) и числовых ID (1001, 2001).
     */
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    /**
     * Роль пользователя в системе (из БД, не из JWT).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * Email пользователя (может измениться в будущем).
     * Хранится для удобства, но НЕ используется как идентификатор.
     */
    @Column(name = "email")
    private String email;

    /**
     * Имя пользователя.
     */
    @Column(name = "name")
    private String name;

    /**
     * Карточка заведения, к которой привязан пользователь.
     * Один VenueProfile может иметь несколько пользователей (Bar Owner, Brew Owner, Global Admin, Location Manager).
     * VenueId заведения можно получить через user.getVenueProfile().getVenueId().
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_profile_id", referencedColumnName = "id")
    private VenueProfile venueProfile;

    /**
     * Количество неявок пользователя.
     */
    @Column(name = "no_show_count")
    @Builder.Default
    private Integer noShowCount = 0;

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
