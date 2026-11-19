package carmarket.backend.service;

import carmarket.backend.repository.AuthRepository;
import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import carmarket.backend.dto.AuditDetails;
import carmarket.backend.security.PasswordPolicy;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapAdminService {
    private final AuthRepository repository;
    private final PasswordPolicy policy;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final Environment environment;
    private final AuditService audit;

    public BootstrapAdminService(AuthRepository repository, PasswordPolicy policy, PasswordEncoder passwords,
                                 JdbcTemplate jdbc, Environment environment, AuditService audit) {
        this.repository = repository;
        this.policy = policy;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.environment = environment;
        this.audit = audit;
    }

    @Transactional
    public void createInitialAdmin() {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(820031751)");
        if (repository.hasActiveAdmin()) return; // Повторный запуск никогда не сбрасывает чужой пароль.
        String login = environment.getProperty("carmarket.bootstrap.login", "");
        String password = environment.getProperty("carmarket.bootstrap.password", "");
        login = PasswordPolicy.normalizeLogin(login);
        if (login.isBlank() || login.length() > 50 || login.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalStateException("Задайте корректный BOOTSTRAP_ADMIN_LOGIN");
        }
        policy.validate(password);
        if (repository.credential(login).isPresent()) {
            throw new IllegalStateException("Логин bootstrap уже существует; выберите новый логин администратора");
        }
        long userId = repository.createAdmin(login, passwords.encode(password));
        audit.recordSystem(AuditAction.BOOTSTRAP_ADMIN_CREATED, AuditTarget.user(userId), AuditDetails.adminCreated());
    }
}
