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
     * Поиск пользователя по externalId (userUuid из JWT).
     * Это надёжный идентификатор, в отличие от email.
     */
    Optional<User> findByExternalId(UUID externalId);

    /**
     * Проверка, существует ли пользователь с указанной ролью.
     * Используется для защиты bootstrap-логики.
     */
    boolean existsByRole(Role role);
}
