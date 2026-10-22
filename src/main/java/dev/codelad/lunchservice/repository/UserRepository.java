package dev.codelad.lunchservice.repository;

import dev.codelad.lunchservice.entity.User;
import dev.codelad.lunchservice.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Поиск пользователя по externalId (userUuid из JWT или user_id как String).
     * Поддерживает UUID (550e8400-e29b-41d4-a716-446655440000)
     * и числовые ID (1001, 2001, 3001).
     */
    Optional<User> findByExternalId(String externalId);

    /**
     * Проверка, существует ли пользователь с указанной ролью.
     * Используется для защиты bootstrap-логики.
     */
    boolean existsByRole(Role role);
}
