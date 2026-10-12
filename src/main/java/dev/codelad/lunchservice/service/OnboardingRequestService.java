package dev.codelad.lunchservice.service;

import dev.codelad.lunchservice.dto.response.OnboardingRequestResponse;
import dev.codelad.lunchservice.entity.OnboardingRequest;
import dev.codelad.lunchservice.entity.User;
import dev.codelad.lunchservice.enums.ComboBookingStatus;
import dev.codelad.lunchservice.exception.OnboardingRequestAlreadyExistsException;
import dev.codelad.lunchservice.exception.OnboardingRequestNotFoundException;
import dev.codelad.lunchservice.repository.OnboardingRequestRepository;
import dev.codelad.lunchservice.repository.UserRepository;
import dev.codelad.lunchservice.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для управления заявками на присоединение к системе Ланчей.
 * Пользователь подает заявку → GL одобряет → пользователь получает роль FRANCHISE_OWNER.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingRequestService {

    private final OnboardingRequestRepository repository;
    private final UserRepository userRepository;

    /**
     * Создать новую заявку от пользователя.
     */
    @Transactional
    public OnboardingRequestResponse create(UUID userId) {
        // Проверка: нет ли уже активной заявки
        if (repository.existsByUserIdAndStatus(userId, ComboBookingStatus.PENDING.name())) {
            throw new OnboardingRequestAlreadyExistsException(userId);
        }

        // Создание заявки
        OnboardingRequest application = OnboardingRequest.builder()
                .userId(userId)
                .status(ComboBookingStatus.PENDING.name())
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();

        OnboardingRequest saved = repository.save(application);
        log.info("[ONBOARDING] Заявка {} создана пользователем {}",
                saved.getId(), userId);

        return OnboardingRequestResponse.from(saved);
    }

    /**
     * Получить все заявки пользователя.
     */
    public List<OnboardingRequestResponse> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OnboardingRequestResponse::from)
                .toList();
    }

    /**
     * Получить все ожидающие рассмотрения заявки.
     */
    public List<OnboardingRequestResponse> findPendingApplications() {
        return repository.findByStatusOrderByCreatedAtAsc(ComboBookingStatus.PENDING.name()).stream()
                .map(OnboardingRequestResponse::from)
                .toList();
    }

    /**
     * Одобрить заявку и назначить пользователя FRANCHISE_OWNER.
     * Пользователь создаётся напрямую (доверяем что он существует в Auth-сервисе).
     */
    @Transactional
    public OnboardingRequestResponse approve(Long applicationId, UUID reviewedBy) {
        OnboardingRequest application = repository.findByIdAndStatus(applicationId, ComboBookingStatus.PENDING.name())
                .orElseThrow(() -> new OnboardingRequestNotFoundException(applicationId));

        // Обновление статуса заявки
        application.setStatus("APPROVED");
        application.setReviewedBy(reviewedBy);
        application.setReviewedAt(LocalDateTime.now(ZoneId.of("UTC")));
        application.setComment("Заявка одобрена");
        repository.save(application);

        // Создание/обновление пользователя с ролью FRANCHISE_OWNER
        // Мы не ищем пользователя в БД, а создаём/сохраняем напрямую,
        // доверяя что он существует в Auth-сервисе
        User user = userRepository.findByExternalId(application.getUserId())
                .orElse(User.builder()
                        .externalId(application.getUserId())
                        .role(Role.ROLE_FRANCHISE_OWNER)
                        .build());
        user.setRole(Role.ROLE_FRANCHISE_OWNER);
        userRepository.save(user);

        log.info("[ONBOARDING] Заявка {} одобрена, пользователь {} назначен FRANCHISE_OWNER",
                application.getId(), application.getUserId());

        return OnboardingRequestResponse.from(application);
    }

    /**
     * Отклонить заявку.
     */
    @Transactional
    public OnboardingRequestResponse reject(Long applicationId, UUID reviewedBy, String comment) {
        OnboardingRequest application = repository.findByIdAndStatus(applicationId, ComboBookingStatus.PENDING.name())
                .orElseThrow(() -> new OnboardingRequestNotFoundException(applicationId));

        application.setStatus("REJECTED");
        application.setReviewedBy(reviewedBy);
        application.setReviewedAt(LocalDateTime.now(ZoneId.of("UTC")));
        application.setComment(comment != null ? comment : "Заявка отклонена");

        OnboardingRequest saved = repository.save(application);
        log.info("[ONBOARDING] Заявка {} отклонена: {}", application.getId(), saved.getComment());

        return OnboardingRequestResponse.from(saved);
    }

    /**
     * Отменить заявку пользователем (до рассмотрения).
     */
    @Transactional
    public void cancelByUser(Long applicationId, UUID userId) {
        OnboardingRequest application = repository.findById(applicationId)
                .orElseThrow(() -> new OnboardingRequestNotFoundException(applicationId));

        if (!application.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Это не ваша заявка");
        }

        if (!ComboBookingStatus.PENDING.name().equals(application.getStatus())) {
            throw new IllegalStateException("Можно отменить только заявку в статусе PENDING");
        }

        repository.delete(application);
        log.info("[ONBOARDING] Заявка {} отменена пользователем {}", applicationId, userId);
    }
}
