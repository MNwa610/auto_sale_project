package carmarket.service;

import carmarket.model.Car;
import carmarket.repository.CarRepository;

import java.util.ArrayList;
import java.util.List;

public class InMemoryCarRepository extends CarRepository {

    private final List<Car> cars = new ArrayList<>();
    private long nextId = 1;

    public void seed(Car car) {
        if (car.getId() == null) {
            car.setId(nextId++);
        }
        cars.add(car);
    }

    @Override
    public Car create(Car car) {
        car.setId(nextId++);
        cars.add(car);
        return car;
    }

    @Override
    public List<Car> findAll() {
        return new ArrayList<>(cars);
    }

    @Override
    public Car findById(Long id) {
        for (Car car : cars) {
            if (car.getId().equals(id)) {
                return car;
            }
        }
        return null;
    }

    @Override
    public boolean update(Car car) {
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).getId().equals(car.getId())) {
                cars.set(i, car);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return cars.removeIf(car -> car.getId().equals(id));
    }
}
