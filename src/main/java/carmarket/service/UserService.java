package carmarket.service;

import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.User;
import carmarket.repository.UserRepository;

import java.util.List;

public class UserService {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;

    public UserService() {
        this(new UserRepository());
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(User user) {
        validateUser(user);
        validateNewPassword(user.getPasswordHash());

        validateUniqueLogin(user);

        return userRepository.create(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        User user = userRepository.findById(id);

        if (user == null) {
            throw new EntityNotFoundException(
                    "Пользователь с id=" + id + " не найден"
            );
        }

        return user;
    }

    public User update(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("Выберите пользователя для изменения.");
        }
        User existing = findById(user.getId());
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(existing.getPasswordHash());
        } else {
            validateNewPassword(user.getPasswordHash());
        }
        user.setCreatedAt(existing.getCreatedAt());
        validateUser(user);
        validateUniqueLogin(user);
        if (!userRepository.update(user)) {
            throw new EntityNotFoundException("Пользователь уже удалён. Обновите таблицу.");
        }
        return user;
    }

    public void delete(Long id) {
        if (userRepository.hasRelatedRecords(id)) {
            throw new BusinessException("Нельзя удалить пользователя: у него есть автомобили или заявки. "
                    + "Сначала удалите связанные записи или измените их пользователя.");
        }
        if (!userRepository.delete(id)) {
            throw new EntityNotFoundException(
                    "Пользователь с id=" + id + " не найден"
            );
        }
    }

    public User findByLogin(String login) {
        return login == null || login.isBlank() ? null : userRepository.findByLogin(login.trim());
    }

    public User authenticate(String login, String password) {
        User user = findByLogin(login);

        if (user == null || !user.getPasswordHash().equals(password)) {
            throw new BusinessException("Неверный логин или пароль");
        }

        return user;
    }

    public User register(String fullName, String login, String password) {
        return saveForm(null, fullName, login, UserRole.USER, password);
    }

    public User saveForm(Long id, String fullName, String login, UserRole role, String password) {
        // Сохраняем способ КР №1: пароль хранится без преобразования.
        User user = new User(fullName, login, password, role);
        user.setId(id);
        return id == null ? create(user) : update(user);
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank()
                || password.length() < MIN_PASSWORD_LENGTH || password.length() > 255) {
            throw new BusinessException("Поле «Пароль»: введите от " + MIN_PASSWORD_LENGTH
                    + " до 255 символов. Пароль не должен состоять только из пробелов.");
        }
    }

    private void validateUniqueLogin(User user) {
        User existing = findByLogin(user.getLogin());
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new BusinessException("Поле «Логин»: этот логин уже занят. Выберите другой.");
        }
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new BusinessException("Пользователь не указан");
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new BusinessException("Поле «ФИО»: укажите фамилию и имя.");
        }

        user.setFullName(user.getFullName().trim());
        if (user.getFullName().length() > 100) {
            throw new BusinessException("Поле «ФИО»: не более 100 символов, сейчас " + user.getFullName().length() + ".");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new BusinessException("Поле «Логин»: укажите логин.");
        }

        user.setLogin(user.getLogin().trim());
        if (user.getLogin().length() > 50) {
            throw new BusinessException("Поле «Логин»: не более 50 символов, сейчас " + user.getLogin().length() + ".");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("Поле «Пароль»: укажите пароль.");
        }
        if (user.getPasswordHash().length() > 255) {
            throw new BusinessException("Поле «Пароль»: не более 255 символов.");
        }

        if (user.getRole() == null) {
            throw new BusinessException("Поле «Роль»: выберите роль из списка.");
        }
    }
}
