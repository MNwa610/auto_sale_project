package carmarket.backend.dto;

import carmarket.backend.domain.RoleCode;
import carmarket.backend.security.SessionUser;

public record CurrentUserResponse(long id, String login, String fullName, RoleCode role,
                                  boolean mustChangePassword) {
    public static CurrentUserResponse from(SessionUser user) {
        return new CurrentUserResponse(user.id(), user.login(), user.fullName(), user.role(), user.mustChangePassword());
    }
}
