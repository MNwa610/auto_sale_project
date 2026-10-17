package carmarket.service;

import carmarket.model.User;
import carmarket.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Простая заглушка репозитория для unit-тестов без PostgreSQL.
 */
public class InMemoryUserRepository extends UserRepository {

    private final List<User> users = new ArrayList<>();
    private long nextId = 1;
    private final Set<Long> usersWithRelations = new HashSet<>();

    public void markUserWithRelations(Long id) {
        usersWithRelations.add(id);
    }

    @Override
    public boolean hasRelatedRecords(Long id) {
        return usersWithRelations.contains(id);
    }

    @Override
    public User findByLogin(String login) {
        return users.stream().filter(user -> user.getLogin().equalsIgnoreCase(login)).findFirst().orElse(null);
    }

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
