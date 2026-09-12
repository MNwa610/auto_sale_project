package carmarket.service;

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
        return userRepository.create(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }

    public User update(User user) {
        if (userRepository.findById(user.getId()) == null) {
            throw new EntityNotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        userRepository.update(user);
        return user;
    }

    public void delete(Long id) {
        if (!userRepository.delete(id)) {
            throw new EntityNotFoundException("Пользователь с id=" + id + " не найден");
        }
    }
}
