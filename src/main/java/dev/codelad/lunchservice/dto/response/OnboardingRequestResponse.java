package dev.codelad.lunchservice.dto.response;

import dev.codelad.lunchservice.entity.OnboardingRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ответ с информацией о заявке на присоединение к системе.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingRequestResponse {

    private Long id;
    private UUID userId;
    private String status;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private String comment;
    private LocalDateTime createdAt;

    public static OnboardingRequestResponse from(OnboardingRequest request) {
        return OnboardingRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .status(request.getStatus())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .comment(request.getComment())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
