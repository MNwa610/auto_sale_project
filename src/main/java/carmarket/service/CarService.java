package carmarket.service;

import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.Car;
import carmarket.repository.CarRepository;
import carmarket.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

public class CarService {

    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public CarService() {
        this(new CarRepository(), new UserRepository());
    }

    public CarService(CarRepository carRepository, UserRepository userRepository) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    public Car create(Car car) {
        validateCar(car);
        checkSellerExists(car.getSellerId());
        return carRepository.create(car);
    }

    public List<Car> findAll() {
        return carRepository.findAll();
    }

    public Car findById(Long id) {
        Car car = carRepository.findById(id);
        if (car == null) {
            throw new EntityNotFoundException("Автомобиль с id=" + id + " не найден");
        }
        return car;
    }

    public Car update(Car car) {
        if (carRepository.findById(car.getId()) == null) {
            throw new EntityNotFoundException("Автомобиль с id=" + car.getId() + " не найден");
        }
        validateCar(car);
        checkSellerExists(car.getSellerId());
        carRepository.update(car);
        return car;
    }

    public void delete(Long id) {
        if (!carRepository.delete(id)) {
            throw new EntityNotFoundException("Автомобиль с id=" + id + " не найден");
        }
    }

    private void validateCar(Car car) {
        if (car.getPrice() == null || car.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цена автомобиля должна быть больше 0");
        }
        if (car.getMileage() < 0) {
            throw new BusinessException("Пробег не может быть отрицательным");
        }
    }

    private void checkSellerExists(Long sellerId) {
        if (userRepository.findById(sellerId) == null) {
            throw new EntityNotFoundException("Продавец с id=" + sellerId + " не найден");
        }
    }
}
