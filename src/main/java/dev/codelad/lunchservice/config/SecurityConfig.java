package dev.codelad.lunchservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codelad.lunchservice.dto.response.ErrorResponse;
import dev.codelad.lunchservice.security.JwtAuthenticationFilter;
import dev.codelad.lunchservice.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация Spring Security.
 * Stateless JWT аутентификация, правила доступа по ролям.
 * 
 * Роли:
 * - GLOBAL_ADMIN — полный доступ ко всему
 * - BAR_OWNER — доступ к своему venueId
 * - BREW_OWNER — доступ к своему venueId
 * - USER — базовый доступ (включая LOCATION_MANAGER через venue_members)
 */
@SuppressWarnings("squid:S4502")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsProperties corsProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Пустой UserDetailsService — отключает UserDetailsServiceAutoConfiguration.
     * Аутентификация осуществляется через JWT-токен.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> null; // never found — JWT is used instead
    }

    /**
     * AuthenticationEntryPoint — возвращает 401 при отсутствии аутентификации.
     * Использует единый формат ErrorResponse для консистентности ответов.
     */
    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            
            ErrorResponse errorResponse = new ErrorResponse(
                    Instant.now().toString(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    "Пользователь не аутентифицирован. Предоставьте JWT-токен в заголовке Authorization: Bearer <token>",
                    request.getRequestURI()
            );
            
            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        };
    }

    /**
     * AccessDeniedHandler — возвращает 403 при отсутствии прав.
     * Использует единый формат ErrorResponse для консистентности ответов.
     */
    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            
            ErrorResponse errorResponse = new ErrorResponse(
                    Instant.now().toString(),
                    HttpStatus.FORBIDDEN.value(),
                    "FORBIDDEN",
                    "У вас нет прав для выполнения этого действия",
                    request.getRequestURI()
            );
            
            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== PUBLIC endpoints (без аутентификации) =====
                        .requestMatchers(
                                "/api/public/**",
                                "/api/v1/auth/login",
                                "/api/test-auth/**",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/v1/lunch/categories"
                        ).permitAll()

                        // ===== PROTECTED endpoints (требуется JWT) =====
                        // Все эндпоинты /api/lunch/** требуют JWT, но детальная проверка прав в PermissionService
                        .requestMatchers("/api/lunch/**").authenticated()
                        .requestMatchers("/api/devices/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // ===== ДОСТУП ПО РОЛЯМ =====
                        .requestMatchers("/api/admin/**").hasRole(Role.ROLE_GLOBAL_ADMIN.getShortName())
                        .requestMatchers("/api/owner/**").hasAnyRole(
                                Role.ROLE_GLOBAL_ADMIN.getShortName(),
                                Role.ROLE_BAR_OWNER.getShortName(),
                                Role.ROLE_BREW_OWNER.getShortName()
                        )
                        .requestMatchers("/api/venue/**").hasRole(Role.ROLE_GLOBAL_ADMIN.getShortName())

                        // Все остальные API требуют аутентификации
                        .requestMatchers("/api/**").authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        List<String> allowedOrigins = corsProperties.getAllowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:8080");
        }

        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}
