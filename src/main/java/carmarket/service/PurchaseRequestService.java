package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.enums.SortField;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.interfaces.Searchable;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.model.TableQuery;
import carmarket.repository.CarRepository;
import carmarket.repository.PurchaseRequestRepository;
import carmarket.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;

public class PurchaseRequestService implements Searchable<PurchaseRequest> {
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;

    public PurchaseRequestService() {
        this(new PurchaseRequestRepository(), new UserRepository(), new CarRepository());
    }

    public PurchaseRequestService(PurchaseRequestRepository purchaseRequestRepository,
                                  UserRepository userRepository,
                                  CarRepository carRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.carRepository = carRepository;
    }

    public PurchaseRequest create(PurchaseRequest request) {
        validateInput(request);
        validateUserExists(request.getUserId());
        Car car = validateCarForRequest(request.getCarId());

        if (car.getStatus() == CarStatus.SOLD || car.getStatus() == CarStatus.ARCHIVED) {
            throw new BusinessException(
                    "Нельзя создать заявку на автомобиль со статусом " + car.getStatus()
            );
        }

        if (hasActiveRequest(request.getUserId(), request.getCarId(), null)) {
            throw new BusinessException(
                    "У пользователя уже есть активная заявка на этот автомобиль"
            );
        }

        request.setStatus(RequestStatus.NEW);

        return purchaseRequestRepository.create(request);
    }

    public List<PurchaseRequest> findAll() {
        return purchaseRequestRepository.findAll();
    }

    public PurchaseRequest findById(Long id) {
        PurchaseRequest request = purchaseRequestRepository.findById(id);
        if (request == null) {
            throw new EntityNotFoundException("Заявка с id=" + id + " не найдена");
        }
        return request;
    }

    public PurchaseRequest update(PurchaseRequest request) {
        validateInput(request);
        PurchaseRequest existing = purchaseRequestRepository.findById(request.getId());
        if (existing == null) {
            throw new EntityNotFoundException(
                    "Заявка с id=" + request.getId() + " не найдена"
            );
        }

        validateUserExists(request.getUserId());
        Car car = validateCarForRequest(request.getCarId());

        if (request.getStatus() != existing.getStatus()) {
            validateStatusTransition(existing.getStatus(), request.getStatus());
        }

        if (!request.getUserId().equals(existing.getUserId())
                || !request.getCarId().equals(existing.getCarId())) {

            if (car.getStatus() == CarStatus.SOLD
                    || car.getStatus() == CarStatus.ARCHIVED) {
                throw new BusinessException(
                        "Нельзя привязать заявку к автомобилю со статусом "
                                + car.getStatus()
                );
            }

            if (hasActiveRequest(
                    request.getUserId(),
                    request.getCarId(),
                    request.getId()
            )) {
                throw new BusinessException(
                        "У пользователя уже есть активная заявка на этот автомобиль"
                );
            }
        }

        if (!purchaseRequestRepository.update(request)) {
            throw new EntityNotFoundException("Заявка уже удалена. Обновите таблицу.");
        }
        return request;
    }

    public List<RequestStatus> allowedStatuses(RequestStatus status) {
        if (status == null) {
            return List.of();
        }
        return switch (status) {
            case NEW -> List.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED);
            case IN_PROGRESS -> List.of(RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED);
            default -> List.of();
        };
    }

    public void changeStatus(Long id, RequestStatus status) {
        PurchaseRequest request = findById(id);
        validateStatusTransition(request.getStatus(), status);
        request.setStatus(status);
        update(request);
    }

    private void validateInput(PurchaseRequest request) {
        if (request == null) {
            throw new BusinessException("Заполните форму заявки.");
        }
        if (request.getUserId() == null) {
            throw new BusinessException("Поле «Пользователь»: выберите пользователя из списка.");
        }
        if (request.getCarId() == null) {
            throw new BusinessException("Поле «Автомобиль»: выберите автомобиль из списка.");
        }
        if (request.getMessage() != null && request.getMessage().length() > 1000) {
            throw new BusinessException("Поле «Сообщение»: не более 1000 символов, сейчас "
                    + request.getMessage().length() + ".");
        }
    }

    public void delete(Long id) {
        if (!purchaseRequestRepository.delete(id)) {
            throw new EntityNotFoundException("Заявка с id=" + id + " не найдена");
        }
    }

    public List<PurchaseRequest> findAllWithDetails() {
        return purchaseRequestRepository.findAllWithDetails();
    }

    @Override
    public List<PurchaseRequest> search(String query) {
        return findForTable(new TableQuery(query, null, null, null, SortField.DEFAULT, true));
    }

    public List<PurchaseRequest> findForTable(TableQuery query) {
        BigDecimal min = SearchFilterService.parsePrice(query.minPrice(), "Цена от");
        BigDecimal max = SearchFilterService.parsePrice(query.maxPrice(), "Цена до");
        SearchFilterService.validatePriceRange(min, max);
        List<PurchaseRequest> result = new ArrayList<>();
        for (PurchaseRequest request : purchaseRequestRepository.findAllWithDetails()) {
            if (SearchFilterService.matches(query.text(), request.getUserFullName(), request.getCarBrand(),
                    request.getCarModel(), request.getMessage())
                    && (query.status() == null || request.getStatus() == query.status())
                    && SearchFilterService.inPriceRange(request.getCarPrice(), min, max)) {
                result.add(request);
            }
        }
        Comparator<PurchaseRequest> comparator = switch (query.sortField() == null ? SortField.DEFAULT : query.sortField()) {
            case PRICE -> Comparator.comparing(PurchaseRequest::getCarPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case MILEAGE -> Comparator.comparing(PurchaseRequest::getCarMileage, Comparator.nullsLast(Comparator.naturalOrder()));
            case DATE -> Comparator.comparing(PurchaseRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case STATUS -> Comparator.comparing(request -> request.getStatus().getDisplayName());
            default -> Comparator.comparing(PurchaseRequest::getId);
        };
        result.sort((query.ascending() ? comparator : comparator.reversed()).thenComparing(PurchaseRequest::getId));
        return result;
    }

    public List<PurchaseRequest> findByUserId(Long userId) {
        validateUserExists(userId);
        return purchaseRequestRepository.findByUserId(userId);
    }

    public List<PurchaseRequest> findByCarId(Long carId) {
        validateCarForRequest(carId);
        return purchaseRequestRepository.findByCarId(carId);
    }

    public List<PurchaseRequest> searchByUserName(String query) {
        List<PurchaseRequest> result = new ArrayList<>();

        if (query == null || query.isBlank()) {
            return result;
        }

        String searchText = query.trim().toLowerCase();

        for (PurchaseRequest request : purchaseRequestRepository.findAll()) {
            User user = userRepository.findById(request.getUserId());

            if (user != null
                    && user.getFullName() != null
                    && user.getFullName().toLowerCase().contains(searchText)) {
                result.add(request);
            }
        }

        return result;
    }

    public List<PurchaseRequest> filterByStatus(RequestStatus status) {
        List<PurchaseRequest> result = new ArrayList<>();

        for (PurchaseRequest request : purchaseRequestRepository.findAll()) {
            if (request.getStatus() == status) {
                result.add(request);
            }
        }

        return result;
    }

    private void validateUserExists(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new EntityNotFoundException(
                    "Пользователь с id=" + userId + " не найден"
            );
        }
    }

    private Car validateCarForRequest(Long carId) {
        Car car = carRepository.findById(carId);
        if (car == null) {
            throw new EntityNotFoundException(
                    "Автомобиль с id=" + carId + " не найден"
            );
        }
        return car;
    }

    private boolean hasActiveRequest(Long userId, Long carId, Long excludeRequestId) {
        for (PurchaseRequest request : purchaseRequestRepository.findAll()) {
            if (excludeRequestId != null
                    && excludeRequestId.equals(request.getId())) {
                continue;
            }

            if (request.getUserId().equals(userId)
                    && request.getCarId().equals(carId)
                    && isActiveStatus(request.getStatus())) {
                return true;
            }
        }

        return false;
    }

    private boolean isActiveStatus(RequestStatus status) {
        return status == RequestStatus.NEW
                || status == RequestStatus.IN_PROGRESS;
    }

    private void validateStatusTransition(RequestStatus from, RequestStatus to) {
        if (from == to) {
            return;
        }

        if (to == null || !allowedStatuses(from).contains(to)) {
            throw new BusinessException(
                    "Недопустимый переход статуса: " + from + " -> " + to
            );
        }
    }
}
