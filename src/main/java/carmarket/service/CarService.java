package carmarket.service;

import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.interfaces.Searchable;
import carmarket.model.Car;
import carmarket.repository.CarRepository;
import carmarket.repository.CatalogRepository;
import carmarket.repository.PurchaseRequestRepository;
import carmarket.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CarService implements Searchable<Car> {

    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final CatalogRepository catalogRepository;
    private final PurchaseRequestRepository requestRepository;

    public CarService() {
        this(new CarRepository(), new UserRepository());
    }

    public CarService(CarRepository carRepository, UserRepository userRepository) {
        this(carRepository, userRepository, new CatalogRepository(), new PurchaseRequestRepository());
    }

    public CarService(CarRepository carRepository, UserRepository userRepository,
                      CatalogRepository catalogRepository, PurchaseRequestRepository requestRepository) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.catalogRepository = catalogRepository;
        this.requestRepository = requestRepository;
    }

    public Car create(Car car) {
        validateCar(car);
        checkSellerExists(car.getSellerId());
        validateCatalogs(car);
        validateUniqueVin(car);
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
        validateCatalogs(car);
        validateUniqueVin(car);
        if (!carRepository.update(car)) {
            throw new EntityNotFoundException("Автомобиль уже удалён. Обновите таблицу.");
        }
        return car;
    }

    public void delete(Long id) {
        if (!requestRepository.findByCarId(id).isEmpty()) {
            throw new BusinessException("Нельзя удалить автомобиль: на него есть заявки. "
                    + "Сначала удалите связанные заявки или измените статус автомобиля на «В архиве».");
        }
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
        if (car == null) {
            throw new BusinessException("Заполните форму автомобиля.");
        }
        if (car.getPrice() == null || car.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цена автомобиля должна быть больше 0");
        }

        if (car.getMileage() < 0) {
            throw new BusinessException("Пробег не может быть отрицательным");
        }
        if (car.getPrice().compareTo(new BigDecimal("9999999999.99")) > 0
                || car.getPrice().stripTrailingZeros().scale() > 2) {
            throw new BusinessException("Поле «Цена»: максимум 9999999999,99 и не более двух знаков после запятой.");
        }
        if (car.getBrand() == null || car.getBrand().isBlank()) {
            throw new BusinessException("Поле «Марка»: выберите марку из списка.");
        }
        if (car.getModel() == null || car.getModel().isBlank()) {
            throw new BusinessException("Поле «Модель»: выберите модель выбранной марки.");
        }
        int maxYear = Math.min(Year.now().getValue() + 1, 2100);
        if (car.getYear() < 1950 || car.getYear() > maxYear) {
            throw new BusinessException("Поле «Год»: укажите целое число от 1950 до " + maxYear + ".");
        }
        if (car.getStatus() == null) {
            throw new BusinessException("Поле «Статус»: выберите статус из списка.");
        }
        if (car.getVin() != null && !car.getVin().isBlank()) {
            car.setVin(car.getVin().trim().toUpperCase(Locale.ROOT));
            if (car.getVin().length() != 17) {
                throw new BusinessException("Поле «VIN»: укажите ровно 17 символов или оставьте поле пустым.");
            }
        } else {
            car.setVin(null);
        }
        if (car.getEngineVolume() == null || car.getEngineVolume().signum() <= 0
                || car.getEngineVolume().compareTo(new BigDecimal("99.9")) > 0
                || car.getEngineVolume().stripTrailingZeros().scale() > 1) {
            throw new BusinessException("Поле «Объём двигателя»: число больше 0 до 99,9, не более одного знака после запятой.");
        }
    }

    private void checkSellerExists(Long sellerId) {
        if (sellerId == null) {
            throw new BusinessException("Поле «Продавец»: выберите пользователя из списка.");
        }
        if (userRepository.findById(sellerId) == null) {
            throw new EntityNotFoundException(
                    "Продавец с id=" + sellerId + " не найден"
            );
        }
    }

    private void validateCatalogs(Car car) {
        boolean modelExists = catalogRepository.findAllModels().stream().anyMatch(model ->
                model.getBrandName().equals(car.getBrand()) && model.getName().equals(car.getModel()));
        if (!modelExists) {
            throw new BusinessException("Поля «Марка» и «Модель»: выберите существующую модель этой марки.");
        }
        if (catalogRepository.findAllBodyTypes().stream().noneMatch(item -> item.getName().equals(car.getBodyType()))) {
            throw new BusinessException("Поле «Кузов»: выберите тип кузова из списка.");
        }
        if (catalogRepository.findAllTransmissions().stream().noneMatch(item -> item.getName().equals(car.getTransmission()))) {
            throw new BusinessException("Поле «Коробка передач»: выберите значение из списка.");
        }
        if (catalogRepository.findAllFuelTypes().stream().noneMatch(item -> item.getName().equals(car.getFuelType()))) {
            throw new BusinessException("Поле «Топливо»: выберите тип топлива из списка.");
        }
    }

    private void validateUniqueVin(Car car) {
        if (car.getVin() == null) {
            return;
        }
        Car existing = carRepository.findByVin(car.getVin());
        if (existing != null && !existing.getId().equals(car.getId())) {
            throw new BusinessException("Поле «VIN»: автомобиль с таким VIN уже существует. Укажите другой VIN.");
        }
    }

    public Car saveForm(Car car, String year, String mileage, String price, String engineVolume) {
        car.setYear(parseInteger(year, "Год"));
        car.setMileage(parseInteger(mileage, "Пробег"));
        car.setPrice(parseDecimal(price, "Цена"));
        car.setEngineVolume(parseDecimal(engineVolume, "Объём двигателя"));
        return car.getId() == null ? create(car) : update(car);
    }

    private int parseInteger(String value, String field) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Поле «" + field + "»: введите целое число в пределах 2147483647.");
        }
    }

    private BigDecimal parseDecimal(String value, String field) {
        String text = value == null ? "" : value.trim().replace(',', '.');
        if (!text.matches("[+-]?\\d+(\\.\\d+)?")) {
            throw new BusinessException("Поле «" + field + "»: введите число, например 2,5.");
        }
        return new BigDecimal(text);
    }
}
