package dev.codelad.lunchservice.entity;

import dev.codelad.lunchservice.security.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность пользователя.
 * externalId — это userUuid из JWT-токена Auth-сервиса.
 * Роль хранится в БД, а не в JWT.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * userUuid из JWT — однозначно уникальный идентификатор из Auth-сервиса.
     * Используется для поиска пользователя, а не email.
     */
    @Column(name = "external_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID externalId;

    /**
     * Роль пользователя в ЭТОМ сервисе (из БД, не из JWT).
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

    @Column(name = "name")
    private String name;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

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
