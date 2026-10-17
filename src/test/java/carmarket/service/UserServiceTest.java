package carmarket.service;

import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {
    private InMemoryUserRepository repository;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        service = new UserService(repository);
    }

    @Test
    void saveForm_shouldCreateUserAndPreserveKr1PasswordBehavior() {
        User user = service.saveForm(null, "  Иван Иванов  ", "  ivan  ", UserRole.USER, "secret1");
        assertThat(user.getId()).isNotNull();
        assertThat(user.getFullName()).isEqualTo("Иван Иванов");
        assertThat(user.getLogin()).isEqualTo("ivan");
        assertThat(service.authenticate("ivan", "secret1").getId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> service.authenticate("ivan", "wrong"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void saveForm_shouldRejectDuplicateLoginIgnoringCaseOnCreateAndEdit() {
        service.saveForm(null, "Первый", "ivan", UserRole.USER, "secret1");
        assertThatThrownBy(() -> service.saveForm(null, "Другой", " IVAN ", UserRole.USER, "secret2"))
                .hasMessageContaining("«Логин»").hasMessageContaining("уже занят");
        User second = service.saveForm(null, "Второй", "petr", UserRole.USER, "secret2");
        assertThatThrownBy(() -> service.saveForm(second.getId(), "Второй", "IVAN", UserRole.USER, ""))
                .hasMessageContaining("уже занят");
        assertThat(repository.findById(second.getId()).getLogin()).isEqualTo("petr");
    }

    @Test
    void saveForm_shouldKeepEmptyPasswordAndRegistrationDateWhenRoleChanges() {
        User user = new User("Иван", "ivan", "12345", UserRole.USER);
        repository.seed(user);
        LocalDateTime registration = LocalDateTime.of(2025, 1, 1, 12, 0);
        user.setCreatedAt(registration);
        User edited = service.saveForm(user.getId(), "Иван Иванов", "IVAN", UserRole.ADMIN, "");
        assertThat(edited.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(edited.getPasswordHash()).isEqualTo("12345");
        assertThat(edited.getCreatedAt()).isEqualTo(registration);
        assertThat(service.authenticate("ivan", "12345").getId()).isEqualTo(user.getId());
    }

    @Test
    void saveForm_shouldReplacePasswordOnlyWhenProvided() {
        User user = service.saveForm(null, "Иван", "ivan", UserRole.USER, "secret1");
        service.saveForm(user.getId(), "Иван", "ivan", UserRole.USER, "new password");
        assertThat(service.authenticate("ivan", "new password").getId()).isEqualTo(user.getId());
        assertThatThrownBy(() -> service.authenticate("ivan", "secret1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void saveForm_shouldRejectMissingOrTooLongNameAndLogin() {
        assertThatThrownBy(() -> service.saveForm(null, " ", "ivan", UserRole.USER, "secret1"))
                .hasMessageContaining("«ФИО»");
        assertThatThrownBy(() -> service.saveForm(null, "я".repeat(101), "ivan", UserRole.USER, "secret1"))
                .hasMessageContaining("сейчас 101");
        assertThatThrownBy(() -> service.saveForm(null, "Иван", " ", UserRole.USER, "secret1"))
                .hasMessageContaining("«Логин»");
        assertThatThrownBy(() -> service.saveForm(null, "Иван", "a".repeat(51), UserRole.USER, "secret1"))
                .hasMessageContaining("сейчас 51");
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void saveForm_shouldAcceptMaximumNameAndLoginLengths() {
        User user = service.saveForm(null, "я".repeat(100), "a".repeat(50), UserRole.USER, "secret1");
        assertThat(user.getId()).isNotNull();
    }

    @Test
    void saveForm_shouldRejectMissingRoleAndInvalidPassword() {
        assertThatThrownBy(() -> service.saveForm(null, "Иван", "ivan", null, "secret1"))
                .hasMessageContaining("«Роль»");
        for (String password : new String[]{null, "", "12345", "      ", "a".repeat(256)}) {
            assertThatThrownBy(() -> service.saveForm(null, "Иван", "ivan", UserRole.USER, password))
                    .hasMessageContaining("«Пароль»");
        }
        User user = service.saveForm(null, "Иван", "ivan", UserRole.USER, "123456");
        assertThatThrownBy(() -> service.saveForm(user.getId(), "Иван", "ivan", UserRole.USER, "short"))
                .hasMessageContaining("«Пароль»");
        assertThat(repository.findById(user.getId()).getPasswordHash()).isEqualTo("123456");
    }

    @Test
    void delete_shouldRejectUserWithCarsOrRequests() {
        User user = service.saveForm(null, "Иван", "ivan", UserRole.USER, "secret1");
        repository.markUserWithRelations(user.getId());
        assertThatThrownBy(() -> service.delete(user.getId())).hasMessageContaining("есть автомобили или заявки");
        assertThat(repository.findById(user.getId())).isNotNull();
    }

    @Test
    void delete_shouldRemoveUserWithoutRelationsAndReportMissingUser() {
        User user = service.saveForm(null, "Иван", "ivan", UserRole.USER, "secret1");
        service.delete(user.getId());
        assertThat(repository.findById(user.getId())).isNull();
        assertThatThrownBy(() -> service.delete(user.getId())).isInstanceOf(EntityNotFoundException.class);
    }
}
