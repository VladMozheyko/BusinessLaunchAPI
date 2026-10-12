package dev.codelad.lunchservice.controller;

import dev.codelad.lunchservice.config.swagger.ApiTags;
import dev.codelad.lunchservice.config.SecurityProperties;
import dev.codelad.lunchservice.dto.request.AuthRequest;
import dev.codelad.lunchservice.dto.response.AuthResponse;
import dev.codelad.lunchservice.config.ProfileUtil;
import dev.codelad.lunchservice.security.Role;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Контроллер для аутентификации и выдачи JWT-токенов.
 * ТОЛЬКО для профилей Dev и Local!
 * 
 * <h3>Тестовые пользователи (предзаполнены в коде):</h3>
 * <table border="1">
 * <tr><th>Email</th><th>Роль</th><th>Описание</th></tr>
 * <tr><td>admin@company.com</td><td>ROLE_GLOBAL_ADMIN</td><td>Глобальный администратор</td></tr>
 * <tr><td>franchise@company.com</td><td>ROLE_FRANCHISE_OWNER</td><td>Владелец франшизы</td></tr>
 * <tr><td>location@company.com</td><td>ROLE_LOCATION_ADMIN</td><td>Администратор локации</td></tr>
 * <tr><td>user@company.com</td><td>ROLE_USER</td><td>Обычный пользователь</td></tr>
 * <tr><td>guest@company.com</td><td>ROLE_GUEST</td><td>Гость</td></tr>
 * </table>
 * 
 * <h3>Использование:</h3>
 * <p>Для получения токена достаточно указать email. UUID генерируется автоматически.</p>
 * <pre>
 * POST /api/v1/auth/login
 * {
 *   "email": "user@company.com"
 * }
 * </pre>
 * 
 * <h3>Важно:</h3>
 * <ul>
 *   <li>Токен действует 15 минут</li>
 *   <li>При каждом запросе генерируется новый UUID</li>
 *   <li>Пользователи НЕ сохраняются в базе данных</li>
 *   <li>Доступно ТОЛЬКО для Dev/Local профилей</li>
 *   <li>В production вернёт ошибку 503</li>
 * </ul>
 */
@Slf4j
@Tag(name = ApiTags.AUTH, description = ApiTags.AUTH_DESC)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SecurityProperties securityProperties;

    /**
     * Список тестовых пользователей с их ролями.
     * Каждый email соответствует определённой роли.
     */
    private static final Set<String> TEST_USERS = Set.of(
            "admin@company.com",
            "franchise@company.com",
            "location@company.com",
            "user@company.com",
            "guest@company.com"
    );

    /**
     * Маппинг email → UUID для тестовых пользователей.
     * UUID генерируется один раз при первом запросе и сохраняется на время жизни приложения.
     * ConcurrentHashMap для потокобезопасности.
     */
    private final Map<String, UUID> emailToUuidMap = new ConcurrentHashMap<>();

    /**
     * Маппинг email на роль.
     */
    private static Role getRoleByEmail(String email) {
        return switch (email) {
            case "admin@company.com" -> Role.ROLE_GLOBAL_ADMIN;
            case "franchise@company.com" -> Role.ROLE_FRANCHISE_OWNER;
            case "location@company.com" -> Role.ROLE_LOCATION_ADMIN;
            case "user@company.com" -> Role.ROLE_USER;
            case "guest@company.com" -> Role.ROLE_GUEST;
            default -> Role.ROLE_USER;
        };
    }

    /**
     * <p>
     * Аутентификация пользователя и получение JWT-токена по email.
     * </p>
     * Логика:
     * 1. Проверяем, что активирован профиль Dev или Local
     * 2. Проверяем, что email есть в списке тестовых пользователей
     * 3. Генерируем новый UUID для каждого запроса
     * 4. Генерируем JWT-токен на 15 минут
     */
    @PostMapping("/login")
    @Operation(
            summary = "Аутентификация и получение JWT-токена",
            description = "Выдаёт JWT-токен на 15 минут для пользователя по email. ТОЛЬКО для Dev/Local профилей. " +
                    "При каждом запросе генерируется новый UUID. Пользователи НЕ сохраняются в БД. " +
                    "<br><br>" +
                    "<b>Доступные тестовые email:</b><br>" +
                    "• admin@company.com → ROLE_GLOBAL_ADMIN<br>" +
                    "• franchise@company.com → ROLE_FRANCHISE_OWNER<br>" +
                    "• location@company.com → ROLE_LOCATION_ADMIN<br>" +
                    "• user@company.com → ROLE_USER<br>" +
                    "• guest@company.com → ROLE_GUEST",
            tags = {ApiTags.AUTH}
    )
    @ApiResponse(responseCode = "200", description = "JWT-токен успешно сгенерирован (действует 15 минут)")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных или недопустимый email")
    @ApiResponse(responseCode = "503", description = "Аутентификация недоступна в production-профиле")
    public ResponseEntity<AuthResponse> authenticate(
            @Parameter(
                    description = "Email тестового пользователя. " +
                            "Доступные email: admin@company.com, franchise@company.com, location@company.com, " +
                            "user@company.com, guest@company.com",
                    required = true
            )
            @Valid @RequestBody AuthRequest request) {

        // Проверяем, что активирован Dev или Local профиль
        String activeProfile =  ProfileUtil.getActiveProfile();
        if (!isDevOrLocalProfile(activeProfile)) {
            log.warn("Аутентификация через AuthController недоступна в профиле: {}", activeProfile);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(buildErrorForProductionProfile());
        }

        // Проверяем, что email есть в списке тестовых пользователей
        if (!TEST_USERS.contains(request.getEmail())) {
            log.warn("Недопустимый email для тестовой аутентификации: {}", request.getEmail());
            return ResponseEntity.badRequest()
                    .body(buildErrorForInvalidEmail());
        }

        log.info("Аутентификация запрослена: {}", request.getEmail());

        // Получаем или генерируем UUID для этого email (стабильный в течение жизни приложения)
        UUID externalId = emailToUuidMap.computeIfAbsent(request.getEmail(), k -> UUID.randomUUID());

        // Получаем роль для этого email
        Role role = getRoleByEmail(request.getEmail());

        // Генерируем JWT-токен на 15 минут
        String token = generateJwtToken(externalId, request.getEmail(), role);

        // Формируем ответ
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(externalId);
        response.setEmail(request.getEmail());
        response.setRole(role.getAuthority());
        response.setExpiresIn(Instant.now().plusSeconds(900)); // 15 минут

        log.info("✓ JWT-токен сгенерирован для пользователя: {} с ролью {}", 
                request.getEmail(), role.getAuthority());

        return ResponseEntity.ok(response);
    }

    /**
     * Генерация JWT-токена на 15 минут с userUuid и email.
     */
    private String generateJwtToken(UUID externalId, String email, Role role) {
        SecretKey key = getSigningKey();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(900); // 15 минут

        return Jwts.builder()
                .setSubject(externalId.toString())
                .claim("userUuid", externalId.toString())
                .claim("email", email)
                .claim("role", role.getAuthority())
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Декодировать Base64 secret key в SecretKey для HMAC.
     * Если ключ не валидный base64 (содержит спецсимволы), хэшируем его через SHA-256.
     */
    private SecretKey getSigningKey() {
        String secret = securityProperties.getSecretKey();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret key not configured!");
        }

        try {
            // Пробуем декодировать как стандартный base64
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        } catch (IllegalArgumentException e) {
            // Если не base64, хэшируем через SHA-256 для получения 32 байт
            log.debug("Secret key не в формате base64, используем SHA-256 хэширование");
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return new SecretKeySpec(keyBytes, "HmacSHA256");
            } catch (Exception hashEx) {
                throw new IllegalStateException("Failed to create signing key from secret", hashEx);
            }
        }
    }

    /**
     * Проверить, что активирован Dev или Local профиль.
     */
    private boolean isDevOrLocalProfile(String profile) {
        return "dev".equalsIgnoreCase(profile) 
                || "local".equalsIgnoreCase(profile)
                || "default".equalsIgnoreCase(profile);
    }

    /**
     * Ответ для production-профиля (когда AuthController недоступен).
     */
    private AuthResponse buildErrorForProductionProfile() {
        AuthResponse response = new AuthResponse();
        response.setToken("ERROR: Authentication not available in production profile");
        return response;
    }

    /**
     * Ответ для недопустимого email.
     */
    private AuthResponse buildErrorForInvalidEmail() {
        AuthResponse response = new AuthResponse();
        response.setToken("ERROR: Invalid test email");
        return response;
    }
}
