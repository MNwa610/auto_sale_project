package carmarket.service;

import carmarket.enums.RequestStatus;
import carmarket.enums.SortField;
import carmarket.interfaces.Searchable;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.TableQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableQueryServiceTest {
    private PurchaseRequestService requestService;
    private CarService carService;
    private InMemoryPurchaseRequestRepository requests;

    @BeforeEach
    void setUp() {
        requests = new InMemoryPurchaseRequestRepository();
        InMemoryCarRepository cars = new InMemoryCarRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        requestService = new PurchaseRequestService(requests, users, cars);
        carService = new CarService(cars, users);
        seedRequest("Иван Иванов", "Toyota", "Camry", "Осмотр вечером", RequestStatus.APPROVED, "600000", 50000, 3);
        seedRequest("Пётр Петров", "Toyota", "Corolla", "Возможен торг?", RequestStatus.NEW, "400000", 90000, 1);
        seedRequest("Иван Иванов", "BMW", "X5", "Без кредита", RequestStatus.APPROVED, "1100000", 30000, 2);
        seedRequest("Елена", "Toyota", "RAV4", "Позвоните", RequestStatus.APPROVED, "700000", 70000, 4);
        for (PurchaseRequest request : requests.findAll()) {
            Car car = new Car();
            car.setBrand(request.getCarBrand());
            car.setModel(request.getCarModel());
            car.setPrice(request.getCarPrice());
            car.setMileage(request.getCarMileage());
            car.setSellerFullName(request.getUserFullName());
            cars.seed(car);
        }
    }

    @Test
    void requestSearch_shouldMatchAllMeaningfulFieldsIgnoringCase() {
        Searchable<PurchaseRequest> searchable = requestService;
        assertThat(searchable.search("  иВаН  ")).extracting(PurchaseRequest::getId).containsExactly(1L, 3L);
        assertThat(searchable.search("toyota camry")).extracting(PurchaseRequest::getId).containsExactly(1L);
        assertThat(searchable.search("COROLLA")).extracting(PurchaseRequest::getId).containsExactly(2L);
        assertThat(searchable.search("ТОРГ")).extracting(PurchaseRequest::getId).containsExactly(2L);
        assertThat(searchable.search("несуществующее")).isEmpty();
    }

    @Test
    void findRequests_shouldCombineSearchStatusPriceAndDescendingMileage() {
        TableQuery query = new TableQuery("Toyota", RequestStatus.APPROVED, "500000", "750000", SortField.MILEAGE, false);
        assertThat(requestService.findForTable(query)).extracting(PurchaseRequest::getId).containsExactly(4L, 1L);
        assertThat(requests.findAll()).extracting(PurchaseRequest::getId).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void priceRange_shouldAllowOneSidedBoundsAndIncludeExactBoundary() {
        assertThat(requestService.findForTable(new TableQuery("", null, "600000,00", "600000.00", SortField.PRICE, true)))
                .extracting(PurchaseRequest::getId).containsExactly(1L);
        assertThat(requestService.findForTable(new TableQuery("", null, "700000", "", SortField.PRICE, true)))
                .extracting(PurchaseRequest::getId).containsExactly(4L, 3L);
        assertThat(requestService.findForTable(new TableQuery("", null, "", "600000", SortField.PRICE, true)))
                .extracting(PurchaseRequest::getId).containsExactly(2L, 1L);
    }

    @Test
    void priceRange_shouldExplainInvalidNumbersAndReversedBounds() {
        assertThatThrownBy(() -> requestService.findForTable(new TableQuery("", null, "abc", "", SortField.DEFAULT, true)))
                .hasMessageContaining("«Цена от»");
        assertThatThrownBy(() -> requestService.findForTable(new TableQuery("", null, "", "-5", SortField.DEFAULT, true)))
                .hasMessageContaining("«Цена до»").hasMessageContaining("отрицательной");
        assertThatThrownBy(() -> requestService.findForTable(new TableQuery("", null, "700000", "500000", SortField.DEFAULT, true)))
                .hasMessageContaining("нижняя граница");
    }

    @Test
    void requestSorting_shouldUseNumbersDatesAndStatusLabels() {
        assertThat(requestService.findForTable(new TableQuery("", null, "", "", SortField.PRICE, true)))
                .extracting(PurchaseRequest::getId).containsExactly(2L, 1L, 4L, 3L);
        assertThat(requestService.findForTable(new TableQuery("", null, "", "", SortField.DATE, false)))
                .extracting(PurchaseRequest::getId).containsExactly(4L, 1L, 3L, 2L);
        assertThat(requestService.findForTable(new TableQuery("", null, "", "", SortField.STATUS, true)))
                .extracting(PurchaseRequest::getId).containsExactly(2L, 1L, 3L, 4L);
    }

    @Test
    void emptyQuery_shouldReturnAllRequestsInInitialOrderAfterFiltering() {
        requestService.findForTable(new TableQuery("Toyota", RequestStatus.APPROVED, "500000", "", SortField.PRICE, false));
        assertThat(requestService.findForTable(TableQuery.empty())).extracting(PurchaseRequest::getId)
                .containsExactly(1L, 2L, 3L, 4L);
        assertThat(requestService.search(null)).hasSize(4);
    }

    @Test
    void carSearchAndSorting_shouldWorkTogetherAndReuseExistingMethods() {
        assertThat(carService.findForTable(new TableQuery("Toyota", null, null, null, SortField.PRICE, true)))
                .extracting(Car::getId).containsExactly(2L, 1L, 4L);
        assertThat(carService.findForTable(new TableQuery("Toyota", null, null, null, SortField.MILEAGE, false)))
                .extracting(Car::getId).containsExactly(2L, 4L, 1L);
        assertThat(carService.search("елена")).extracting(Car::getId).containsExactly(4L);
        assertThat(carService.sortByPrice(false)).extracting(Car::getId).containsExactly(3L, 4L, 1L, 2L);
        assertThat(carService.sortByMileage(true)).extracting(Car::getId).containsExactly(3L, 1L, 4L, 2L);
        assertThat(carService.search("  ")).extracting(Car::getId).containsExactly(1L, 2L, 3L, 4L);
    }

    private void seedRequest(String name, String brand, String model, String message,
                             RequestStatus status, String price, int mileage, int day) {
        PurchaseRequest request = new PurchaseRequest(1L, 1L, message, status);
        request.setUserFullName(name);
        request.setCarBrand(brand);
        request.setCarModel(model);
        request.setCarPrice(new BigDecimal(price));
        request.setCarMileage(mileage);
        request.setCreatedAt(LocalDateTime.of(2026, 1, day, 12, 0));
        requests.seed(request);
    }
}
