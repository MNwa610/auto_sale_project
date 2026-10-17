package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.UserRole;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.Car;
import carmarket.model.CarModel;
import carmarket.model.NamedCatalogItem;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.enums.RequestStatus;
import carmarket.repository.CatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryCarRepository carRepository;
    private CarService carService;
    private Long sellerId;
    private InMemoryPurchaseRequestRepository requestRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        carRepository = new InMemoryCarRepository();
        requestRepository = new InMemoryPurchaseRequestRepository();
        carService = new CarService(carRepository, userRepository, new TestCatalogRepository(), requestRepository);

        User seller = new User("Иван Иванов", "seller1", "hash", UserRole.USER);
        userRepository.seed(seller);
        sellerId = seller.getId();
    }

    @Test
    void create_shouldFailWhenPriceIsZeroOrNegative() {
        Car car = sampleCar(sellerId, BigDecimal.ZERO, 10000);

        BusinessException exception = assertThrows(BusinessException.class, () -> carService.create(car));
        assertEquals("Цена автомобиля должна быть больше 0", exception.getMessage());
    }

    @Test
    void create_shouldFailWhenMileageIsNegative() {
        Car car = sampleCar(sellerId, new BigDecimal("500000"), -1);

        BusinessException exception = assertThrows(BusinessException.class, () -> carService.create(car));
        assertEquals("Пробег не может быть отрицательным", exception.getMessage());
    }

    @Test
    void create_shouldFailWhenSellerNotFound() {
        Car car = sampleCar(999L, new BigDecimal("500000"), 10000);

        assertThrows(EntityNotFoundException.class, () -> carService.create(car));
    }

    @Test
    void create_shouldSucceedWhenDataIsValid() {
        Car car = sampleCar(sellerId, new BigDecimal("850000.00"), 45000);

        Car created = carService.create(car);

        assertNotNull(created.getId());
        assertEquals("Toyota", created.getBrand());
    }

    @Test
    void saveForm_shouldExplainInvalidPriceAndAcceptDecimalComma() {
        Car car = sampleCar(sellerId, new BigDecimal("850000"), 0);
        assertThatThrownBy(() -> carService.saveForm(car, "2020", "100", "abc", "2,5"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("«Цена»");
        Car saved = carService.saveForm(car, "2020", "0", "850000,25", "2,5");
        assertThat(saved.getPrice()).isEqualByComparingTo("850000.25");
        assertThat(saved.getEngineVolume()).isEqualByComparingTo("2.5");
    }

    @Test
    void create_shouldRejectYearOutsideAllowedRange() {
        Car car = sampleCar(sellerId, new BigDecimal("850000"), 0);
        car.setYear(1949);
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Год»");
        car.setYear(Year.now().getValue() + 2);
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Год»");
    }

    @Test
    void create_shouldRejectInvalidOrDuplicateVin() {
        Car car = sampleCar(sellerId, new BigDecimal("850000"), 0);
        car.setVin("short");
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("17 символов");
        car.setVin("JTNB11HK003000001");
        carService.create(car);
        Car duplicate = sampleCar(sellerId, new BigDecimal("800000"), 0);
        duplicate.setVin("  jtnb11hk003000001  ");
        assertThatThrownBy(() -> carService.create(duplicate)).hasMessageContaining("таким VIN уже существует");
        assertThat(carRepository.findAll()).hasSize(1);
    }

    @Test
    void update_shouldAllowSameVinAndNewPrice() {
        Car original = sampleCar(sellerId, new BigDecimal("850000"), 100);
        original.setVin("JTNB11HK003000001");
        carService.create(original);
        Car edited = sampleCar(sellerId, new BigDecimal("750000"), 100);
        edited.setId(original.getId());
        edited.setVin(original.getVin());
        carService.update(edited);
        assertThat(carRepository.findById(original.getId()).getPrice()).isEqualByComparingTo("750000");
    }

    @Test
    void create_shouldRejectModelFromAnotherBrandAndMissingCatalogValue() {
        Car car = sampleCar(sellerId, new BigDecimal("850000"), 0);
        car.setBrand("Kia");
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Марка» и «Модель»");
        car.setBrand("Toyota");
        car.setBodyType(null);
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Кузов»");
    }

    @Test
    void create_shouldRejectEngineVolumeAndPriceThatDoNotFitDatabase() {
        Car car = sampleCar(sellerId, new BigDecimal("850000"), 0);
        car.setEngineVolume(BigDecimal.ZERO);
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Объём двигателя»");
        car.setEngineVolume(new BigDecimal("2.55"));
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Объём двигателя»");
        car.setEngineVolume(new BigDecimal("2.5"));
        car.setPrice(new BigDecimal("10000000000"));
        assertThatThrownBy(() -> carService.create(car)).hasMessageContaining("«Цена»");
    }

    @Test
    void delete_shouldRejectCarWithAnyRequestIncludingFinalStatus() {
        Car car = carService.create(sampleCar(sellerId, new BigDecimal("850000"), 0));
        requestRepository.seed(new PurchaseRequest(sellerId, car.getId(), "Заявка", RequestStatus.REJECTED));
        assertThatThrownBy(() -> carService.delete(car.getId())).hasMessageContaining("есть заявки");
        assertThat(carRepository.findById(car.getId())).isNotNull();
    }

    @Test
    void delete_shouldRemoveCarWithoutRequests() {
        Car car = carService.create(sampleCar(sellerId, new BigDecimal("850000"), 0));
        carService.delete(car.getId());
        assertThat(carRepository.findById(car.getId())).isNull();
    }

    private static class TestCatalogRepository extends CatalogRepository {
        @Override
        public List<CarModel> findAllModels() {
            CarModel model = new CarModel();
            model.setBrandName("Toyota");
            model.setName("Camry");
            return List.of(model);
        }

        @Override
        public List<NamedCatalogItem> findAllBodyTypes() {
            return List.of(new NamedCatalogItem(1L, "sedan"));
        }

        @Override
        public List<NamedCatalogItem> findAllTransmissions() {
            return List.of(new NamedCatalogItem(1L, "automatic"));
        }

        @Override
        public List<NamedCatalogItem> findAllFuelTypes() {
            return List.of(new NamedCatalogItem(1L, "petrol"));
        }
    }

    private Car sampleCar(Long sellerId, BigDecimal price, int mileage) {
        return new Car(
                sellerId,
                "Toyota",
                "Camry",
                2019,
                mileage,
                price,
                null,
                "sedan",
                "automatic",
                "petrol",
                new BigDecimal("2.5"),
                "Описание",
                CarStatus.AVAILABLE
        );
    }
}
