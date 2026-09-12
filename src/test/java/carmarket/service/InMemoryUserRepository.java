package carmarket.service;

import carmarket.model.User;
import carmarket.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Простая заглушка репозитория для unit-тестов без PostgreSQL.
 */
public class InMemoryUserRepository extends UserRepository {

    private final List<User> users = new ArrayList<>();
    private long nextId = 1;

    public void seed(User user) {
        if (user.getId() == null) {
            user.setId(nextId++);
        }
        users.add(user);
    }

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.add(user);
        return user;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public User findById(Long id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public boolean update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return users.removeIf(user -> user.getId().equals(id));
    }
}
