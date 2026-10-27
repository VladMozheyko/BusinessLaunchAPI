package dev.codelad.lunchservice.security;

import dev.codelad.lunchservice.config.GlobalAdminProperties;
import dev.codelad.lunchservice.config.SecurityProperties;
import dev.codelad.lunchservice.dto.response.ErrorResponse;
import dev.codelad.lunchservice.entity.User;
import dev.codelad.lunchservice.entity.VenueProfile;
import dev.codelad.lunchservice.repository.UserRepository;
import dev.codelad.lunchservice.repository.VenueProfileRepository;
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
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.Base64;

/**
 * <p>
 * Фильтр для валидации JWT токенов от двух источников:
 * 1. Auth-сервис (Base Auth) — JWT с userUuid, email. Роль определяется по bootstrap email или берётся из БД.
 * 2. Bar Auth-сервис — JWT с user_id, user_type ("bar"/"brew"/"admin"), bar_id/brew_id.
 * </p>
 * <p>
 * Логика работы:
 * 1. Пробуем валидировать токен с secretKey (Auth-сервис)
 * 2. Если не получилось — пробуем barAuthSecretKey (Bar Auth-сервис)
 * 3. Если валидация прошла с secretKey → роль берём из БД (Bootstrap: bootstrap email → GLOBAL_ADMIN, иначе → USER)
 * 4. Если валидация прошла с barAuthSecretKey → роль определяем по user_type claim
 * </p>
 * <p>
 * Bootstrap-привязка пользователя при первом входе (только для Auth-сервиса):
 * 1. Ищем пользователя по externalId (userUuid из JWT).
 * 2. Если не найден — проверяем email:
 *    - Если email совпадает с app.admin.bootstrap-email и GLOBAL_ADMIN ещё не создан → создаём GLOBAL_ADMIN.
 *    - Иначе → создаём ROLE_USER.
 * 3. Bootstrap срабатывает только один раз за всю жизнь системы.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final UserRepository userRepository;
    private final GlobalAdminProperties globalAdminProperties;
    private final VenueProfileRepository venueProfileRepository;

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

        // Пытаемся валидировать токен и определить источник
        TokenValidationResult validationResult = validateTokenWithSource(token);
        
        if (validationResult == null || !validationResult.isValid()) {
            log.warn("[JWT] Недействительный токен для запроса: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser authenticatedUser = extractAuthenticatedUserWithSource(
                token, validationResult.getJwtSource());
                
        if (authenticatedUser != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            authenticatedUser.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("[JWT] Пользователь аутентифицирован: externalId={}, role={}, source={}",
                    authenticatedUser.getExternalId(),
                    authenticatedUser.getRole(),
                    validationResult.getJwtSource());
        } else {
            log.warn("[JWT] Не удалось извлечь AuthenticatedUser для запроса: {}", request.getRequestURI());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "Пользователь не аутентифицирован. Предоставьте JWT-токен в заголовке Authorization: Bearer <token>", request.getRequestURI());
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
     * Вспомогательный класс для результата валидации токена.
     */
    private static class TokenValidationResult {
        private final boolean valid;
        private final JwtSource jwtSource;

        public TokenValidationResult(boolean valid, JwtSource jwtSource) {
            this.valid = valid;
            this.jwtSource = jwtSource;
        }

        public boolean isValid() {
            return valid;
        }

        public JwtSource getJwtSource() {
            return jwtSource;
        }
    }

    /**
     * Источник JWT токена.
     */
    private enum JwtSource {
        AUTH_SERVICE,     // Auth-сервис (роль из БД)
        BAR_AUTH_SERVICE  // Bar Auth-сервис (роль из user_type claim)
    }

    /**
     * Валидировать токен и определить источник.
     * Сначала пробуем secretKey (Auth-сервис), затем barAuthSecretKey (Bar Auth-сервис).
     *
     * @param token JWT токен
     * @return результат валидации или null если токен недействительный
     */
    private TokenValidationResult validateTokenWithSource(String token) {
        // 1. Пробуем валидировать с secretKey (Auth-сервис)
        try {
            SecretKey authKey = getSigningKey(securityProperties.getSecretKey());
            if (authKey != null) {
                Jwts.parserBuilder()
                        .setSigningKey(authKey)
                        .setAllowedClockSkewSeconds(60)
                        .build()
                        .parseClaimsJws(token);
                log.debug("[JWT] Токен валидирован через Auth-сервис");
                return new TokenValidationResult(true, JwtSource.AUTH_SERVICE);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[JWT] Токен не валидирован с secretKey (Auth-сервис): {}", e.getMessage());
        }

        // 2. Пробуем валидировать с barAuthSecretKey (Bar Auth-сервис)
        try {
            String barAuthKey = securityProperties.getBarAuthSecretKey();
            if (barAuthKey != null && !barAuthKey.isBlank()) {
                SecretKey barAuthSigningKey = getSigningKey(barAuthKey);
                if (barAuthSigningKey != null) {
                    Jwts.parserBuilder()
                            .setSigningKey(barAuthSigningKey)
                            .setAllowedClockSkewSeconds(60)
                            .build()
                            .parseClaimsJws(token);
                    log.debug("[JWT] Токен валидирован через Bar Auth-сервис");
                    return new TokenValidationResult(true, JwtSource.BAR_AUTH_SERVICE);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[JWT] Токен не валидирован с barAuthSecretKey (Bar Auth-сервис): {}", e.getMessage());
        }

        // 3. Если ни один ключ не подошёл
        log.warn("[JWT] Токен не валидирован ни с одним ключом");
        return null;
    }

    /**
     * Извлечь AuthenticatedUser из claims токена с учётом источника.
     * 
     * @param token JWT токен
     * @param jwtSource источник токена (AUTH_SERVICE или BAR_AUTH_SERVICE)
     * @return AuthenticatedUser или null если не удалось
     */
    private AuthenticatedUser extractAuthenticatedUserWithSource(String token, JwtSource jwtSource) {
        try {
            SecretKey key;
            if (jwtSource == JwtSource.BAR_AUTH_SERVICE) {
                // Bar Auth-сервис — используем barAuthSecretKey
                key = getSigningKey(securityProperties.getBarAuthSecretKey());
            } else {
                // Auth-сервис — используем secretKey
                key = getSigningKey(securityProperties.getSecretKey());
            }

            if (key == null) {
                log.error("[JWT] Не удалось получить signing key для источника: {}", jwtSource);
                return null;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (jwtSource == JwtSource.BAR_AUTH_SERVICE) {
                // Bar Auth-сервис — определяем роль по user_type claim
                return extractFromBarAuth(claims);
            } else {
                // Auth-сервис — роль берём из БД
                return extractFromAuthService(claims);
            }
        } catch (Exception e) {
            log.error("[JWT] Не удалось извлечь AuthenticatedUser из токена (источник: {}): {}", 
                    jwtSource, e.getMessage());
            return null;
        }
    }

    /**
     * Извлечь AuthenticatedUser из Bar Auth JWT.
     * Роль определяется по user_type claim:
     * - "admin" → GLOBAL_ADMIN
     * - "bar" → BAR_OWNER
     * - "brew" → BREW_OWNER
     */
    private AuthenticatedUser extractFromBarAuth(Claims claims) {
        // Извлекаем user_id как Long (TestAuthController генерирует Long)
        Long authUserIdLong = claims.get("user_id", Long.class);
        String authUserIdStr = authUserIdLong != null ? authUserIdLong.toString() : null;
        
        String userType = claims.get("user_type", String.class);
        String email = claims.get("email", String.class);

        if (authUserIdStr == null || userType == null) {
            log.error("[JWT] Не удалось извлечь user_id или user_type из Bar Auth JWT");
            return null;
        }

        Long authUserId = authUserIdLong;

        // externalId теперь String — используем authUserIdStr напрямую
        String externalId = authUserIdStr;

        // Определяем роль по user_type
        Long venueId = claims.get("bar_id", Long.class);
        if (venueId == null) {
            venueId = claims.get("brew_id", Long.class);
        }

        Role role;
        switch (userType) {
            case "admin" -> role = Role.ROLE_GLOBAL_ADMIN;
            case "bar" -> role = Role.ROLE_BAR_OWNER;
            case "brew" -> role = Role.ROLE_BREW_OWNER;
            default -> {
                log.error("[JWT] Неизвестный user_type в Bar Auth JWT: {}", userType);
                return null;
            }
        }

        // Ищем пользователя в БД по externalId (создаём если не найден)
        // Использу final переменные для lambda
        final Role finalRole = role;
        final Long finalVenueId = venueId;
        final String finalExternalId = externalId;
        final String finalEmail = email;
        
        User user = userRepository.findByExternalId(finalExternalId)
                .orElseGet(() -> bootstrapUserFromBarAuth(finalExternalId, finalEmail, finalRole, finalVenueId));

        // Обновляем lastLoginAt
        Instant updatedAt = Instant.now();
        user.setUpdatedAt(updatedAt);
        userRepository.save(user);

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.getAuthority()));

        log.info("[JWT] Пользователь аутентифицирован через Bar Auth: authUserId={}, role={}, venueId={}",
                authUserId, role.getAuthority(), venueId);

        return AuthenticatedUser.builder()
                .userId(null)
                .externalId(externalId) // externalId теперь String
                .email(email)
                .role(role)
                .authorities(authorities)
                .authUserId(authUserId)
                .userType(userType)
                .venueId(venueId)
                .venueProfile(user.getVenueProfile())
                .build();
    }

    /**
     * Извлечь AuthenticatedUser из Auth-сервис JWT.
     * Роль берётся из БД по externalId.
     */
    private AuthenticatedUser extractFromAuthService(Claims claims) {
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

        // externalId теперь String
        String externalIdStr = userUuid;
        UUID externalId = UUID.fromString(userUuid);
        String email = claims.get("email", String.class);

        // Ищем пользователя в БД по externalId (создаём если не найден)
        User user = userRepository.findByExternalId(externalIdStr)
                .orElseGet(() -> bootstrapUser(externalIdStr, email));

        Role role = user.getRole();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.getAuthority()));

        log.debug("[JWT] Пользователь найден в БД (Auth-сервис): externalId={}, role={}",
                externalIdStr, role.getAuthority());

        return AuthenticatedUser.builder()
                .userId(null)
                .externalId(externalIdStr)
                .email(email)
                .role(role)
                .authorities(authorities)
                .venueProfile(user.getVenueProfile())
                .build();
    }

    /**
     * Bootstrap-привязка пользователя при первом входе (Auth-сервис).
     */
    private User bootstrapUser(String externalIdStr, String email) {
        log.info("[BOOTSTRAP] Пользователь не найден в БД, создаём нового: externalId={}", externalIdStr);

        Role role;
        boolean globalAdminExists = userRepository.existsByRole(Role.ROLE_GLOBAL_ADMIN);

        if (email != null
                && email.equalsIgnoreCase(globalAdminProperties.getBootstrapEmail())
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
                .externalId(externalIdStr)
                .email(email)
                .role(role)
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Bootstrap-привязка пользователя из Bar Auth-сервиса при первом входе.
     * Роль определяется по user_type claim из JWT.
     * НЕ создаёт VenueProfile — он создаётся лениво при первом запросе к данным заведения.
     */
    private User bootstrapUserFromBarAuth(String externalIdStr, String email, Role role, Long venueId) {
        log.info("[BAR_AUTH_BOOTSTRAP] Пользователь не найден, создаём нового: externalId={}, role={}, venueId={}",
                externalIdStr, role, venueId);

        User newUser = User.builder()
                .externalId(externalIdStr)
                .email(email)
                .role(role)
                .venueProfile(null) // VenueProfile будет создан лениво
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Записать ErrorResponse в ответ.
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                path
        );
        
        PrintWriter writer = response.getWriter();
        writer.write(new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(errorResponse));
        writer.flush();
    }

    /**
     * Декодировать Base64 secret key в SecretKey для HMAC.
     * Если ключ не валидный base64 (содержит спецсимволы), хэшируем его через SHA-256.
     *
     * @param secret секретная строка
     * @return SecretKey или null если ошибка
     */
    private SecretKey getSigningKey(String secret) {
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
