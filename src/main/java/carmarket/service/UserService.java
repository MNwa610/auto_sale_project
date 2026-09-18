package carmarket.service;

import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.User;
import carmarket.repository.UserRepository;

import java.util.List;

public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        this(new UserRepository());
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(User user) {
        validateUser(user);

        if (findByLogin(user.getLogin()) != null) {
            throw new BusinessException("Пользователь с таким логином уже существует");
        }

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
        if (userRepository.findById(user.getId()) == null) {
            throw new EntityNotFoundException(
                    "Пользователь с id=" + user.getId() + " не найден"
            );
        }

        validateUser(user);
        userRepository.update(user);
        return user;
    }

    public void delete(Long id) {
        if (!userRepository.delete(id)) {
            throw new EntityNotFoundException(
                    "Пользователь с id=" + id + " не найден"
            );
        }
    }

    public User findByLogin(String login) {
        for (User user : userRepository.findAll()) {
            if (user.getLogin().equalsIgnoreCase(login)) {
                return user;
            }
        }

        return null;
    }

    public User authenticate(String login, String password) {
        User user = findByLogin(login);

        if (user == null || !user.getPasswordHash().equals(password)) {
            throw new BusinessException("Неверный логин или пароль");
        }

        return user;
    }

    public User register(String fullName, String login, String password) {
        if (fullName.isBlank()) {
            throw new BusinessException("ФИО не может быть пустым");
        }

        if (login.isBlank()) {
            throw new BusinessException("Логин не может быть пустым");
        }

        if (password.isBlank()) {
            throw new BusinessException("Пароль не может быть пустым");
        }

        User user = new User(fullName, login, password, UserRole.USER);
        return create(user);
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new BusinessException("Пользователь не указан");
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new BusinessException("ФИО не может быть пустым");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new BusinessException("Логин не может быть пустым");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("Пароль не может быть пустым");
        }

        if (user.getRole() == null) {
            throw new BusinessException("Роль пользователя не указана");
        }
    }
}
