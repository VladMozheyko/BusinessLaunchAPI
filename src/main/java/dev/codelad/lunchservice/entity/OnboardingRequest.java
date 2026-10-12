package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Заявка на присоединение к системе Ланчей.
 * Пользователь подает заявку → GL одобряет → пользователь получает роль FRANCHISE_OWNER.
 */
@Entity
@Table(name = "onboarding_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID пользователя из Auth-сервиса (externalId).
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Статус заявки: PENDING, APPROVED, REJECTED.
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    /**
     * ID Global Admin'а, рассмотревшего заявку.
     */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("UTC"));
}
