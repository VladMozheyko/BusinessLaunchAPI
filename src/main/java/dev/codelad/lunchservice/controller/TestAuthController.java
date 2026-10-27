package dev.codelad.lunchservice.controller;


import dev.codelad.lunchservice.config.ProfileUtil;
import dev.codelad.lunchservice.config.SecurityProperties;
import dev.codelad.lunchservice.config.swagger.ApiTags;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Тестовый контроллер для выдачи JWT-токенов.
 * ТОЛЬКО для профилей Dev и Local!
 * <p>
 * Эмулирует работу двух auth сервисов:
 * 1. Auth Service — выдаёт токены с userUuid (UUID), email для regular users
 * 2. Bar Auth Service — выдаёт токены с user_id (Long), user_type, bar_id/brew_id для bar/brewery owners
 * 
 * <h3>Auth Service (regular users):</h3>
 * <table border="1">
 * <tr><th>Email</th><th>Роль</th><th>Описание</th></tr>
 * <tr><td>admin@beer.com</td><td>ROLE_ADMIN</td><td>Глобальный администратор</td></tr>
 * <tr><td>user@beer.com</td><td>ROLE_USER</td><td>Обычный пользователь</td></tr>
 * </table>
 * 
 * <h3>Bar Auth Service (bar/brewery owners):</h3>
 * <table border="1">
 * <tr><th>user_type</th><th>bar_id</th><th>brew_id</th><th>Описание</th></tr>
 * <tr><td>bar</td><td>1</td><td>null</td><td>Владелец бара</td></tr>
 * <tr><td>brew</td><td>null</td><td>1</td><td>Владелец пивоварни</td></tr>
 * <tr><td>admin</td><td>1</td><td>null</td><td>Админ бара</td></tr>
 * </table>
 * 
 * <h3>Использование:</h3>
 * <p>Выберите email/userType из выпадающего списка в Swagger UI и нажмите "Try it out" → "Execute".</p>
 * 
 * <h3>Важно:</h3>
 * <ul>
 *   <li>Токены действуют 15 минут</li>
 *   <li>При каждом запросе генерируется новый UUID</li>
 *   <li>Пользователи НЕ сохраняются в базе данных</li>
 *   <li>Доступно ТОЛЬКО для Dev/Local профилей</li>
 *   <li>В production вернёт ошибку 503</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/test-auth")
@RequiredArgsConstructor
@Tag(name = ApiTags.AUTH, description = ApiTags.AUTH_DESC)
public class TestAuthController {

    private final SecurityProperties securityProperties;

    // ==================== Auth Service (regular users) ====================

    /**
     * Статичные UUID для тестовых пользователей.
     * Фиксированные значения, чтобы externalId не менялись при перезапуске.
     */
    private static final Map<String, UUID> AUTH_SERVICE_UUIDS = Map.of(
            "admin@beer.com", UUID.fromString("a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"),
            "user1@beer.com", UUID.fromString("b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1"),
            "user2@beer.com", UUID.fromString("c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2"),
            "user3@beer.com", UUID.fromString("d3d3d3d3-d3d3-d3d3-d3d3-d3d3d3d3d3d3")
    );

    private static final Map<String, UUID> authServiceUuidMap = new ConcurrentHashMap<>(AUTH_SERVICE_UUIDS);

    /**
     * Получить токен Auth Service (regular users).
     * <p>
     * Используется параметр email из выпадающего списка.
     */
    @PostMapping("/auth-service/token")
    @Operation(
            summary = "Получить токен Auth Service (regular users)",
            description = "Эмулирует Auth Service для regular users.<br><br>" +
                    "<b>Выберите email из списка и нажмите Execute:</b><br>" +
                    "<table border=\"1\" style=\"margin: 10px 0;\">\n" +
                    "<tr><th>Email</th><th>Роль</th><th>Описание</th></tr>\n" +
                    "<tr><td>admin@beer.com</td><td>ROLE_ADMIN</td><td>Глобальный администратор</td></tr>\n" +
                    "<tr><td>user1@beer.com</td><td>ROLE_USER</td><td>Обычный пользователь (пример 1)</td></tr>\n" +
                    "<tr><td>user2@beer.com</td><td>ROLE_USER</td><td>Обычный пользователь (пример 2)</td></tr>\n" +
                    "<tr><td>user3@beer.com</td><td>d3d3d3d3-...</td><td>ROLE_USER</td><td>Обычный пользователь</td></tr>\n" +
                    "</table><br>" +
                    "<b>Важно:</b><br>" +
                    "• UUID фиксированные для каждого email (не меняются при перезапуске)<br>" +
                    "<b>Claims в JWT:</b><br>" +
                    "• role (String) — роль (ROLE_ADMIN, ROLE_USER)<br>" +
                    "• userUuid (UUID) — ID пользователя<br>" +
                    "• deviceId (UUID) — ID устройства<br>" +
                    "• sub (String) — email пользователя<br><br>" +
                    "<b>Время жизни токена:</b> 15 минут (900 сек)<br>" +
                    "<b>Refresh token:</b> 30 дней (2592000 сек)"
    )
    @ApiResponse(responseCode = "200", description = "Токены успешно сгенерированы")
    @ApiResponse(responseCode = "503", description = "Недоступно в production")
    public ResponseEntity<AuthServiceResponse> getAuthServiceToken(
            @Parameter(
                    description = "Email пользователя для аутентификации",
                    required = true,
                    example = "admin@beer.com",
                    allowEmptyValue = false
            )
            @RequestParam @Schema(
                    description = "Email пользователя (UUID фиксированные)",
                    allowableValues = {"admin@beer.com", "user1@beer.com", "user2@beer.com", "user3@beer.com"},
                    example = "user1@beer.com",
                    required = true
            ) String email) {
        
        if (!isDevOrLocalProfile()) {
            log.warn("TestAuthController недоступен в production профиле: {}", ProfileUtil.getActiveProfile());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AuthServiceResponse("ERROR: Authentication not available in production"));
        }

        if (!isValidAuthUser(email)) {
            log.warn("Недопустимый email для Auth Service: {}", email);
            return ResponseEntity.badRequest()
                    .body(new AuthServiceResponse("ERROR: Invalid email. Use: admin@beer.com, user1@beer.com, user2@beer.com, user3@beer.com"));
        }

        UUID userUuid = authServiceUuidMap.get(email);
        String role = getRoleByEmail(email);
        String deviceId = UUID.randomUUID().toString();
        
        // Генерируем access token (15 минут)
        String accessToken = generateAuthServiceAccessToken(userUuid, email, role, deviceId);
        // Генерируем refresh token (30 дней)
        String refreshToken = generateRefreshToken();

        log.info("✓ Auth Service токены выданы: email={}, role={}, userUuid={}", email, role, userUuid);
        
        return ResponseEntity.ok(new AuthServiceResponse(
                accessToken,
                refreshToken,
                deviceId,
                userUuid,
                "Успешная аутентификация",
                email,
                "200"
        ));
    }

    // ==================== Bar Auth Service (bar/brewery owners) ====================

    /**
     * Статичные Long ID для Bar Auth пользователей.
     * Фиксированные значения, чтобы не менялись при перезапуске.
     */
    private static final Map<String, Long> BAR_AUTH_IDS = Map.of(
            "bar", 1001L,
            "brew", 2001L,
            "admin", 3001L
    );

    private static final Map<String, Long> barAuthUserIdMap = new ConcurrentHashMap<>(BAR_AUTH_IDS);

    /**
     * Получить токен Bar Auth Service (bar/brewery owners).
     * <p>
     * Используется параметр userType из выпадающего списка.
     */
    @PostMapping("/bar-auth-service/token")
    @Operation(
            summary = "Получить токен Bar Auth Service (bar/brewery owners)",
            description = "Эмулирует Bar Auth Service для владельцев баров и пивоварен.<br><br>" +
                    "<b>Выберите userType из списка и нажмите Execute:</b><br>" +
                    "<table border=\"1\" style=\"margin: 10px 0;\">\n" +
                    "<tr><th>user_type</th><th>user_id</th><th>bar_id</th><th>brew_id</th><th>Описание</th></tr>\n" +
                    "<tr><td>bar</td><td>1001</td><td>1</td><td>null</td><td>Владелец бара</td></tr>\n" +
                    "<tr><td>brew</td><td>2001</td><td>null</td><td>2</td><td>Владелец пивоварни</td></tr>\n" +
                    "<tr><td>admin</td><td>3001</td><td>null</td><td>null</td><td>Админ бара</td></tr>\n" +
                    "</table><br>" +
                    "<b>Важно:</b><br>" +
                    "• user_id фиксированные (не меняются при перезапуске)<br>" +
                    "<b>Claims в JWT:</b><br>" +
                    "• user_id (Long) — ID владельца<br>" +
                    "• user_type (String) — тип: admin, bar, brew<br>" +
                    "• bar_id (Long, nullable) — ID бара<br>" +
                    "• brew_id (Long, nullable) — ID пивоварни<br>" +
                    "• iss (String) — issuer: admin.center.beer<br>" +
                    "• aud (String) — audience: admin.center.beer.consumer<br><br>" +
                    "<b>Время жизни токена:</b> 15 минут (900 сек)<br>" +
                    "<b>Refresh token:</b> 30 дней (2592000 сек)"
    )
    @ApiResponse(responseCode = "200", description = "Токены успешно сгенерированы")
    @ApiResponse(responseCode = "503", description = "Недоступно в production")
    public ResponseEntity<BarAuthServiceResponse> getBarAuthServiceToken(
            @Parameter(
                    description = "Тип пользователя для аутентификации",
                    required = true,
                    example = "bar"
            )
            @RequestParam @Schema(
                    description = "Тип пользователя",
                    allowableValues = {"bar", "brew", "admin"},
                    example = "bar",
                    required = true
            ) String userType) {
        
        if (!isDevOrLocalProfile()) {
            log.warn("TestAuthController недоступен в production профиле: {}", ProfileUtil.getActiveProfile());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new BarAuthServiceResponse("ERROR: Authentication not available in production"));
        }

        if (!isValidBarAuthUserType(userType)) {
            log.warn("Недопустимый userType для Bar Auth Service: {}", userType);
            return ResponseEntity.badRequest()
                    .body(new BarAuthServiceResponse("ERROR: Invalid userType. Use: bar, brew, admin"));
        }

        Long userId = barAuthUserIdMap.get(userType);
        BarAuthClaims claims = getBarAuthClaims(userType);
        
        // Генерируем access token (15 минут)
        String accessToken = generateBarAuthServiceAccessToken(userId, claims);
        // Генерируем refresh token (30 дней)
        String refreshToken = generateRefreshToken();

        log.info("✓ Bar Auth Service токены выданы: userType={}, userId={}, barId={}, brewId={}", 
                userType, userId, claims.barId, claims.brewId);
        
        return ResponseEntity.ok(new BarAuthServiceResponse(
                accessToken,
                "Bearer",
                900,
                refreshToken,
                2592000,
                claims
        ));
    }

    // ==================== Expired Tokens (для тестирования 401) ====================

    /**
     * Получить истёкший токен Bar Auth Service.
     * Полезно для тестирования обработки 401 Unauthorized.
     */
    @PostMapping("/bar-auth-service/expired-token")
    @Operation(
            summary = "Получить истёкший токен Bar Auth Service",
            description = "Возвращает JWT токен с истёкшим сроком действия (expired=1 час назад). " +
                    "Используется для тестирования обработки 401 Unauthorized."
    )
    @ApiResponse(responseCode = "200", description = "Истёкший токен сгенерирован")
    public ResponseEntity<BarAuthServiceResponse> getExpiredBarAuthToken(
            @RequestParam(defaultValue = "bar") @Schema(
                    description = "Тип пользователя",
                    allowableValues = {"bar", "brew", "admin"},
                    example = "bar"
            ) String userType) {
        
        if (!isDevOrLocalProfile()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new BarAuthServiceResponse("ERROR: Authentication not available in production"));
        }

        if (!isValidBarAuthUserType(userType)) {
            return ResponseEntity.badRequest()
                    .body(new BarAuthServiceResponse("ERROR: Invalid userType. Use: bar, brew, admin"));
        }

        Long userId = barAuthUserIdMap.getOrDefault("expired_" + userType, barAuthUserIdMap.get(userType));
        BarAuthClaims claims = getBarAuthClaims(userType);
        
        // Генерируем истёкший токен (expired = 1 час назад)
        String expiredToken = generateExpiredBarAuthServiceAccessToken(userId, claims);

        log.info("✓ Истёкший Bar Auth токен выдан: userType={}, userId={}", userType, userId);
        
        return ResponseEntity.ok(new BarAuthServiceResponse(
                expiredToken,
                "Bearer",
                900,
                "expired-refresh-token",
                2592000,
                claims
        ));
    }

    /**
     * Генерация истёкшего JWT токена для Bar Auth Service.
     */
    private String generateExpiredBarAuthServiceAccessToken(Long userId, BarAuthClaims claims) {
        SecretKey key = getSigningKey(securityProperties.getBarAuthSecretKey());
        Instant now = Instant.now();
        Instant expiry = now.minusSeconds(3600); // истёк 1 час назад

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("user_id", userId)
                .claim("user_type", claims.userType)
                .claim("bar_id", claims.barId)
                .claim("brew_id", claims.brewId)
                .claim("iss", "admin.center.beer")
                .claim("aud", "admin.center.beer.consumer")
                .setIssuedAt(java.util.Date.from(now.minusSeconds(3600)))
                .setExpiration(java.util.Date.from(expiry))
                .signWith(key)
                .compact();
    }

    // ==================== Helpers ====================

    /**
     * Проверка, что активен Dev или Local профиль.
     */
    private boolean isDevOrLocalProfile() {
        String profile = ProfileUtil.getActiveProfile();
        return "dev".equalsIgnoreCase(profile)
                || "local".equalsIgnoreCase(profile)
                || "default".equalsIgnoreCase(profile);
    }

    /**
     * Валидация email для Auth Service.
     */
    private boolean isValidAuthUser(String email) {
        return "admin@beer.com".equalsIgnoreCase(email) 
                || "user1@beer.com".equalsIgnoreCase(email)
                || "user2@beer.com".equalsIgnoreCase(email)
                || "user3@beer.com".equalsIgnoreCase(email);
    }

    /**
     * Валидация userType для Bar Auth Service.
     */
    private boolean isValidBarAuthUserType(String userType) {
        return "bar".equals(userType) || "brew".equals(userType) || "admin".equals(userType);
    }

    /**
     * Получение роли по email для Auth Service.
     */
    private String getRoleByEmail(String email) {
        return "admin@beer.com".equalsIgnoreCase(email) ? "ROLE_ADMIN" : "ROLE_USER";
    }

    /**
     * Генерация JWT токена для Auth Service (access token).
     * Claims: role, userUuid, deviceId, sub, iat, exp
     */
    private String generateAuthServiceAccessToken(UUID userUuid, String email, String role, String deviceId) {
        SecretKey key = getSigningKey(securityProperties.getSecretKey());
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(900); // 15 минут

        return Jwts.builder()
                .setSubject(email) // sub = email
                .claim("role", role) // ROLE_ADMIN, ROLE_USER
                .claim("userUuid", userUuid.toString())
                .claim("deviceId", deviceId)
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Генерация JWT токена для Bar Auth Service.
     * Claims: user_id (Long), user_type, bar_id, brew_id, iss, aud, iat, exp
     */
    private String generateBarAuthServiceAccessToken(Long userId, BarAuthClaims claims) {
        SecretKey key = getSigningKey(securityProperties.getBarAuthSecretKey());
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(900); // 15 минут

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("user_id", userId)
                .claim("user_type", claims.userType)
                .claim("bar_id", claims.barId)
                .claim("brew_id", claims.brewId)
                .claim("iss", "admin.center.beer")
                .claim("aud", "admin.center.beer.consumer")
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Генерация opaque refresh token (случайная строка).
     */
    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Получение claims для Bar Auth Service по userType.
     */
    private BarAuthClaims getBarAuthClaims(String userType) {
        return switch (userType) {
            case "bar" -> new BarAuthClaims("bar", 1L, null);
            case "brew" -> new BarAuthClaims("brew", null, 2L);
            case "admin" -> new BarAuthClaims("admin", null, null);
            default -> new BarAuthClaims("bar", 1L, null);
        };
    }

    /**
     * Декодирование secret key в SecretKey для HMAC.
     */
    private SecretKey getSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret key not configured!");
        }

        try {
            // Пробуем декодировать как стандартный base64
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        } catch (IllegalArgumentException e) {
            // Если не base64, хэшируем через SHA-256
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return new SecretKeySpec(keyBytes, "HmacSHA256");
            } catch (Exception hashEx) {
                throw new IllegalStateException("Failed to create signing key", hashEx);
            }
        }
    }

    // ==================== DTOs ====================

    /**
     * Ответ Auth Service (соответствует реальному формату).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthServiceResponse {
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "JWT access token (действует 15 минут)",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9BRE1JTiIsInVzZXJVdWlkIjoiNTYyMjc4MDQtOGZmNi00ZDhiLWE4MTktODM1Y2E3YmQ4YWJjIiwiZGV2aWNlSWQiOiJlZWYyY2YxMy05OTRlLTQyODUtODU2OC0xNmMwM2JjNmY4NDgiLCJzdWIiOiJWbGFkNzIyMjlAeWFuZGV4LnJ1IiwiaWF0IjoxNzkyMTgzNDk4LCJleHAiOjE3OTIxODQzOTh9.48iZjqfe05iF040lMXBEdOzX8Mm_YC3t8hCW48GJsjY"
        )
        private String accessToken;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Opaque refresh token (действует 30 дней)",
                example = "MmZhYTkwZjktNTU2OC00MzA0LTg3ODgtNGU5NzA3N2Y0YWIw"
        )
        private String refreshToken;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "UUID устройства",
                example = "eef2cf13-994e-4285-8568-16c03bc6f848"
        )
        private String deviceId;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "UUID пользователя из Auth Service",
                example = "56227804-8ff6-4d8b-a819-835ca7bd8abc"
        )
        private UUID userUuid;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Сообщение о статусе",
                example = "Успешная аутентификация"
        )
        private String message;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Email пользователя",
                example = "Vlad72229@yandex.ru"
        )
        private String email;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Статус ответа",
                example = "200"
        )
        private String status;

        /** Конструктор для error responses */
        public AuthServiceResponse(String errorMessage) {
            this.accessToken = errorMessage;
        }
    }

    /**
     * Ответ Bar Auth Service (соответствует реальному формату).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarAuthServiceResponse {
        @io.swagger.v3.oas.annotations.media.Schema(
                description = "JWT access token (действует 15 минут)",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxLCJ1c2VyX3R5cGUiOiJiYXIiLCJiYXJfaWQiOjEsImlhdCI6MTc5MjEzOTUwMiwiaXNzIjoiYWRtaW4uY2VudGVyLmJlZXIiLCJhdWQiOiJhZG1pbi5jZW50ZXIuYmVlci5jb25zdW1lciIsImV4cCI6MTc5MjE0MDQwMn0.b3lgE0P1ZGx25hrAayJAFkp3A6WCUWFHX_WAHhafnzk"
        )
        private String access_token;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Тип токена",
                example = "Bearer"
        )
        private String token_type;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Время жизни access token в секундах",
                example = "900"
        )
        private int expires_in;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Opaque refresh token (действует 30 дней)",
                example = "heiJKJK4ZQ2khpJYSMzxHdY3COJ_RA08M32Mxrg0DaIYdKVIujf5lZnCaAi7n3ak"
        )
        private String refresh_token;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Время жизни refresh token в секундах (30 дней = 2592000)",
                example = "2592000"
        )
        private int refresh_expires_in;

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Claims из JWT токена",
                example = "{\"user_id\": 1, \"user_type\": \"bar\", \"bar_id\": 1}"
        )
        private BarAuthClaims claims;

        /** Конструктор для error responses */
        public BarAuthServiceResponse(String errorMessage) {
            this.access_token = errorMessage;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class BarAuthClaims {
        private String userType;
        private Long barId;
        private Long brewId;
    }
}
