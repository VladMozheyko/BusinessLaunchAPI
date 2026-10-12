package dev.codelad.lunchservice.config;

import dev.codelad.lunchservice.security.JwtAuthenticationFilter;
import dev.codelad.lunchservice.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.util.List;

/**
 * Конфигурация Spring Security.
 * Stateless JWT аутентификация, правила доступа по ролям.
 */
@SuppressWarnings("squid:S4502")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ===== PROTECTED endpoints (требуется JWT) =====
                        .requestMatchers("/api/lunch/**").authenticated()

                        // ===== ДОСТУП ПО РОЛЯМ =====
                        .requestMatchers("/api/admin/**").hasRole(Role.ROLE_GLOBAL_ADMIN.getShortName())
                        .requestMatchers("/api/franchise/**").hasAnyRole(Role.ROLE_GLOBAL_ADMIN.getShortName(), Role.ROLE_FRANCHISE_OWNER.getShortName())
                        .requestMatchers("/api/location/**").hasAnyRole(Role.ROLE_GLOBAL_ADMIN.getShortName(), Role.ROLE_LOCATION_ADMIN.getShortName())

                        // Все остальные API требуют аутентификации
                        .requestMatchers("/api/**").authenticated()
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
