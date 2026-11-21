package carmarket.backend.controller;

import carmarket.backend.domain.RoleCode;
import carmarket.backend.dto.PageResponse;
import carmarket.backend.security.SessionUser;
import carmarket.backend.service.AdminUserService;
import carmarket.backend.service.AdminUserService.Account;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    public record Create(@NotBlank @Size(max = 50) String login, @NotBlank @Size(max = 100) String fullName,
                         @NotNull RoleCode role, String temporaryPassword) {
        @Override public String toString() { return "Create[redacted]"; }
    }
    public record Update(@NotNull RoleCode role, @NotNull Boolean enabled, @NotNull @PositiveOrZero Long version) { }
    public record ResetPassword(String temporaryPassword, @NotNull @PositiveOrZero Long version) {
        @Override public String toString() { return "ResetPassword[redacted]"; }
    }

    private final AdminUserService users;

    public AdminUserController(AdminUserService users) { this.users = users; }

    @GetMapping
    public PageResponse<Account> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return users.list(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@AuthenticationPrincipal SessionUser actor, @Valid @RequestBody Create request) {
        return users.create(actor, request.login(), request.fullName(), request.role(), request.temporaryPassword());
    }

    @PutMapping("/{id}")
    public Account update(@AuthenticationPrincipal SessionUser actor, @PathVariable long id,
                          @Valid @RequestBody Update request) {
        return users.update(actor, id, request.role(), request.enabled(), request.version());
    }

    @PostMapping("/{id}/temporary-password")
    public Account reset(@AuthenticationPrincipal SessionUser actor, @PathVariable long id,
                         @Valid @RequestBody ResetPassword request) {
        return users.resetPassword(actor, id, request.temporaryPassword(), request.version());
    }

    @PostMapping("/{id}/revoke-sessions")
    public int revoke(@AuthenticationPrincipal SessionUser actor, @PathVariable long id) {
        return users.revokeSessions(actor, id);
    }
}
