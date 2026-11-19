package carmarket.backend.controller;

import carmarket.backend.dto.*;
import carmarket.backend.security.SessionUser;
import carmarket.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Авторизация")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    @Operation(summary = "Вход по логину и паролю")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokens(service.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновить access-токен с ротацией refresh-токена")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return tokens(service.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Отозвать семейство refresh-сессии; повторный выход допустим")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        service.logout(request);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Получить текущего пользователя")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal SessionUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(CurrentUserResponse.from(user));
    }

    @PostMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Сменить пароль, отозвать старые сессии и получить новую пару токенов")
    public ResponseEntity<TokenResponse> changePassword(@AuthenticationPrincipal SessionUser user,
                                                       @Valid @RequestBody ChangePasswordRequest request) {
        return tokens(service.changePassword(user, request));
    }

    private ResponseEntity<TokenResponse> tokens(TokenResponse tokens) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
    }
}
