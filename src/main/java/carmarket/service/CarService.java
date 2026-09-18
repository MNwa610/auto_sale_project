package carmarket.service;

import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.interfaces.Searchable;
import carmarket.model.Car;
import carmarket.repository.CarRepository;
import carmarket.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CarService implements Searchable<Car> {

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

    @Override
    public List<Car> search(String query) {
        List<Car> result = new ArrayList<>();

        if (query == null || query.isBlank()) {
            return result;
        }

        String searchText = query.trim().toLowerCase();

        for (Car car : carRepository.findAll()) {
            String brand = car.getBrand() == null ? "" : car.getBrand().toLowerCase();
            String model = car.getModel() == null ? "" : car.getModel().toLowerCase();

            if (brand.contains(searchText)
                    || model.contains(searchText)
                    || (brand + " " + model).contains(searchText)) {
                result.add(car);
            }
        }

        return result;
    }

    public List<Car> filterByPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null || maxPrice == null) {
            throw new BusinessException("Нужно указать минимальную и максимальную цену");
        }

        if (minPrice.compareTo(BigDecimal.ZERO) < 0
                || maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Цена не может быть отрицательной");
        }

        if (minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(
                    "Минимальная цена не может быть больше максимальной"
            );
        }

        List<Car> result = new ArrayList<>();

        for (Car car : carRepository.findAll()) {
            if (car.getPrice() != null
                    && car.getPrice().compareTo(minPrice) >= 0
                    && car.getPrice().compareTo(maxPrice) <= 0) {
                result.add(car);
            }
        }

        return result;
    }

    public List<Car> sortByPrice(boolean ascending) {
        List<Car> cars = new ArrayList<>(carRepository.findAll());

        Comparator<Car> comparator = Comparator.comparing(Car::getPrice);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        cars.sort(comparator);
        return cars;
    }

    public List<Car> sortByMileage(boolean ascending) {
        List<Car> cars = new ArrayList<>(carRepository.findAll());

        Comparator<Car> comparator = Comparator.comparingInt(Car::getMileage);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        cars.sort(comparator);
        return cars;
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
            throw new EntityNotFoundException(
                    "Продавец с id=" + sellerId + " не найден"
            );
        }
    }
}
