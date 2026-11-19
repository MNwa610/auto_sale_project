package carmarket.backend.service;

import carmarket.backend.repository.AuthRepository;
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

    public BootstrapAdminService(AuthRepository repository, PasswordPolicy policy, PasswordEncoder passwords,
                                 JdbcTemplate jdbc, Environment environment) {
        this.repository = repository;
        this.policy = policy;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.environment = environment;
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
        repository.createAdmin(login, passwords.encode(password));
    }
}
