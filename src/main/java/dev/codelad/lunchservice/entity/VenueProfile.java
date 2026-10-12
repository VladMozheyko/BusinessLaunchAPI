package dev.codelad.lunchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Карточка заведения (бизнес-ланча).
 * Принадлежит франчайзи, содержит название, описание, расписание и логотип.
 */
@Entity
@Table(name = "venue_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID пользователя (франчайзи), которому принадлежит карточка.
     */
    @Column(name = "franchise_user_id", nullable = false)
    private UUID franchiseUserId;

    /**
     * ID заведения в системе (уникальный идентификатор заведения).
     */
    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    /**
     * Название бизнес-ланча.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Дополнительный текст с описанием (максимум 500 символов).
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * URL логотипа заведения.
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * Активна ли карточка (видна пользователям для бронирования).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Максимальная вместимость заведения (количество гостей).
     */
    @Column(name = "capacity")
    private Integer capacity;
}
