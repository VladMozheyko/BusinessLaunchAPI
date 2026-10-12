package dev.codelad.lunchservice.security;

import dev.codelad.lunchservice.config.GlobalAdminProperties;
import dev.codelad.lunchservice.config.SecurityProperties;
import dev.codelad.lunchservice.entity.User;
import dev.codelad.lunchservice.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.Base64;

/**
 * <p>
 * Фильтр для валидации JWT токенов от Auth-сервиса.
 * Извлекает userUuid из claims, загружает User из БД и устанавливает SecurityContext.
 * Роль берётся из БД, а не из JWT.
 * </p>
 * Логика проверки глобального админа:
 * 1. Ищем пользователя по externalId (userUuid из JWT).
 * 2. Если не найден — проверяем email:
 *    - Если email совпадает с app.admin.bootstrap-email и GLOBAL_ADMIN ещё не создан → создаём GLOBAL_ADMIN.
 *    - Иначе → создаём ROLE_USER.
 * 3. Bootstrap срабатывает только один раз за всю жизнь системы.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final GlobalAdminProperties globalAdminProperties;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        // Пропускаем публичные эндпоинты без JWT
        String uri = request.getRequestURI();
        if (isPublicEndpoint(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token == null) {
            log.debug("[JWT] Токен не найден в запросе: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (!validateToken(token)) {
            log.warn("[JWT] Недействительный токен для запроса: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser authenticatedUser = extractAuthenticatedUser(token);
        if (authenticatedUser != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            authenticatedUser.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[JWT] Пользователь аутентифицирован: userId={}, externalId={}, role={}",
                    authenticatedUser.getUserId(),
                    authenticatedUser.getExternalId(),
                    authenticatedUser.getRole());
        } else {
            log.warn("[JWT] Не удалось извлечь AuthenticatedUser для запроса: {}", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Недействительный токен\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/actuator/")
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/swagger-ui/")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources/")
                || uri.startsWith("/webjars/");
    }

    /**
     * Извлечь JWT токен из заголовка Authorization
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(securityProperties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(securityProperties.getPrefix())) {
            return header.substring(securityProperties.getPrefix().length());
        }
        log.debug("[JWT] Заголовок Authorization пуст или не имеет Bearer-префикса");
        return null;
    }

    /**
     * Валидировать токен и извлечь claims.
     */
    private boolean validateToken(String token) {
        try {
            SecretKey key = getSigningKey();
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Недействительный токен: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Извлечь AuthenticatedUser из claims токена.
     * Загружает роль из БД по externalId (userUuid).
     * Если пользователь не найден — применяет bootstrap-логику.
     */
    private AuthenticatedUser extractAuthenticatedUser(String token) {
        try {
            SecretKey key = getSigningKey();
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Извлекаем userUuid из JWT
            String userUuid = claims.get("userUuid", String.class);
            if (userUuid == null) {
                String subject = claims.getSubject();
                if (subject != null) {
                    userUuid = subject;
                }
            }
            if (userUuid == null) {
                log.error("[JWT] Не удалось извлечь userUuid из токена");
                return null;
            }

            UUID externalId = UUID.fromString(userUuid);

            // Извлекаем email из JWT (может отличаться от email в БД)
            String email = claims.get("email", String.class);

            // Ищем пользователя в БД по externalId
            User user = userRepository.findByExternalId(externalId)
                    .orElseGet(() -> bootstrapUser(externalId, email));

            // Создаём authorities на основе роли из БД
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(user.getRole().getAuthority()));

            return AuthenticatedUser.builder()
                    .userId(user.getId())
                    .externalId(user.getExternalId())
                    .email(email)
                    .role(user.getRole())
                    .authorities(authorities)
                    .build();
        } catch (Exception e) {
            log.error("[JWT] Не удалось извлечь AuthenticatedUser из токена: {}", e.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * Bootstrap-привязка пользователя при первом входе.
     * </p>
     * Логика:
     * 1. Если email совпадает с app.admin.bootstrap-email И GLOBAL_ADMIN ещё не создан → ROLE_GLOBAL_ADMIN.
     * 2. Иначе → ROLE_USER.
     * 3. Bootstrap срабатывает только один раз за всю жизнь системы.
     */
    private User bootstrapUser(UUID externalId, String email) {
        log.info("[BOOTSTRAP] Пользователь не найден в БД, создаём нового: externalId={}", externalId);

        Role role;
        boolean globalAdminExists = userRepository.existsByRole(Role.ROLE_GLOBAL_ADMIN);

        if (email != null
                && email.equalsIgnoreCase(globalAdminProperties.getDefaultEmail())
                && !globalAdminExists) {
            // Это первый вход глобального админа
            log.info("[BOOTSTRAP] Создаём GLOBAL_ADMIN: email={}", email);
            role = Role.ROLE_GLOBAL_ADMIN;
        } else {
            // Обычный пользователь или GLOBAL_ADMIN уже создан
            log.info("[BOOTSTRAP] Создаём ROLE_USER: email={}", email);
            role = Role.ROLE_USER;
        }

        User newUser = User.builder()
                .externalId(externalId)
                .email(email)
                .role(role)
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Декодировать Base64 secret key в SecretKey для HMAC.
     * Если ключ не валидный base64 (содержит спецсимволы), хэшируем его через SHA-256.
     */
    private SecretKey getSigningKey() {
        String secret = securityProperties.getSecretKey();
        if (secret == null || secret.isBlank()) {
            log.error("JWT secret key not configured!");
            return null;
        }

        try {
            // Пробуем декодировать как стандартный base64
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        } catch (IllegalArgumentException e) {
            // Если не base64, хэшируем через SHA-256 для получения 32 байт
            log.debug("JWT secret key не в формате base64, используем SHA-256 хэширование");
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return new SecretKeySpec(keyBytes, "HmacSHA256");
            } catch (Exception hashEx) {
                log.error("Failed to create signing key from secret", hashEx);
                return null;
            }
        }
    }
}
