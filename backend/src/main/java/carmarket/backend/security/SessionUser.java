package carmarket.backend.security;

import carmarket.backend.domain.RoleCode;
import java.util.UUID;

/** Состояние пользователя и сессии читается из БД, а не принимается из claims роли. */
public record SessionUser(long id, UUID sessionId, String login, String fullName,
                          RoleCode role, boolean mustChangePassword) {
}
