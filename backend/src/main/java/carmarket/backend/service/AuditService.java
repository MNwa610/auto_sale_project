package carmarket.backend.service;

import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import carmarket.backend.dto.AuditDetails;
import carmarket.backend.repository.AuditRepository;
import carmarket.backend.security.SessionUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.Objects;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AuditService {
    private final AuditRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AuditService(AuditRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public void recordAsCurrentUser(AuditAction action, AuditTarget target, AuditDetails details) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SessionUser user)) {
            throw new IllegalStateException("Для пользовательского аудита требуется текущий пользователь");
        }
        recordAsUser(user.id(), action, target, details);
    }

    /** Для сервисов, которые сами проверили учётные данные до появления SecurityContext. */
    public void recordAsUser(long actorId, AuditAction action, AuditTarget target, AuditDetails details) {
        if (actorId <= 0) throw new IllegalArgumentException("Некорректный автор аудита");
        append(actorId, action, target, details);
    }

    /** Явное системное действие; отсутствие авторизации не превращаем в SYSTEM автоматически. */
    public void recordSystem(AuditAction action, AuditTarget target, AuditDetails details) {
        append(null, action, target, details);
    }

    private void append(Long actorId, AuditAction action, AuditTarget target, AuditDetails details) {
        Objects.requireNonNull(action, "Действие аудита обязательно");
        Objects.requireNonNull(target, "Объект аудита обязателен");
        Objects.requireNonNull(details, "Метаданные аудита обязательны");
        try {
            repository.append(actorId, action, target, mapper.writeValueAsString(details), clock.instant());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось подготовить метаданные аудита", exception);
        }
    }
}
