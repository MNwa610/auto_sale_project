package carmarket.backend.config;

import carmarket.backend.repository.AuthRepository;
import carmarket.backend.security.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean public Clock clock() { return Clock.systemUTC(); }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public SecretKey jwtKey(AuthProperties properties) {
        try {
            byte[] bytes = Base64.getDecoder().decode(properties.secretBase64());
            if (bytes.length < 32) throw new IllegalArgumentException();
            return new SecretKeySpec(bytes, "HmacSHA256");
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 должен содержать Base64-ключ минимум из 32 случайных байт");
        }
    }

    @Bean public JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key, AuthProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> purpose = jwt -> jwt.getAudience() != null && jwt.getAudience().contains("carmarket-api")
                && "access".equals(jwt.getClaimAsString("token_use")) && jwt.getExpiresAt() != null
                && jwt.getExpiresAt().isAfter(clock.instant())
                && jwt.getIssuedAt() != null && !jwt.getIssuedAt().isAfter(clock.instant())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator(properties.issuer()), purpose));
        return decoder;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthRepository repository, Clock clock,
                                                    SecurityProblemWriter problems) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(access -> access
                        .requestMatchers(HttpMethod.GET, "/api/v1", "/actuator/health", "/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/password").authenticated()
                        .requestMatchers("/api/v1/admin/**", "/api/v1/users/**", "/api/v1/audit/**")
                            .access(roles("ADMIN"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").access(roles("VIEWER", "MANAGER", "ADMIN"))
                        .requestMatchers("/api/v1/catalogs/**").access(roles("ADMIN"))
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cars/*").access(roles("ADMIN"))
                        .requestMatchers("/api/v1/**").access(roles("MANAGER", "ADMIN"))
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> problems.write(request, response,
                                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Требуется действительный access-токен."))
                        .accessDeniedHandler((request, response, exception) -> {
                            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                            boolean temporary = auth != null && auth.getPrincipal() instanceof SessionUser user
                                    && user.mustChangePassword();
                            problems.write(request, response, HttpStatus.FORBIDDEN,
                                    temporary ? "PASSWORD_CHANGE_REQUIRED" : "FORBIDDEN",
                                    temporary ? "Необходимо сменить временный пароль." : "Недостаточно прав.");
                        }))
                .oauth2ResourceServer(resource -> resource
                        .authenticationEntryPoint((request, response, exception) -> problems.write(request, response,
                                HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Access-токен недействителен или истёк."))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                            try {
                                long id = Long.parseLong(token.getSubject());
                                UUID sessionId = UUID.fromString(token.getClaimAsString("sid"));
                                return new SessionAuthentication(repository.sessionUser(id, sessionId, clock.instant())
                                        .orElseThrow(() -> new OAuth2AuthenticationException("invalid_token")));
                            } catch (IllegalArgumentException | NullPointerException exception) {
                                throw new OAuth2AuthenticationException("invalid_token");
                            }
                        })));
        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> roles(String... allowed) {
        return (authentication, request) -> {
            var auth = authentication.get();
            boolean granted = auth.isAuthenticated() && auth.getPrincipal() instanceof SessionUser user
                    && !user.mustChangePassword() && Arrays.asList(allowed).contains(user.role().name());
            return new AuthorizationDecision(granted);
        };
    }
}
