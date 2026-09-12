package carmarket.service;

import carmarket.enums.CarStatus;
import carmarket.enums.RequestStatus;
import carmarket.exception.BusinessException;
import carmarket.exception.EntityNotFoundException;
import carmarket.model.Car;
import carmarket.model.PurchaseRequest;
import carmarket.model.User;
import carmarket.repository.CarRepository;
import carmarket.repository.PurchaseRequestRepository;
import carmarket.repository.UserRepository;

import java.util.List;

public class PurchaseRequestService {

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
        validateUserExists(request.getUserId());
        Car car = validateCarForRequest(request.getCarId());

        if (car.getStatus() == CarStatus.SOLD || car.getStatus() == CarStatus.ARCHIVED) {
            throw new BusinessException("Нельзя создать заявку на автомобиль со статусом " + car.getStatus());
        }

        if (hasActiveRequest(request.getUserId(), request.getCarId(), null)) {
            throw new BusinessException("У пользователя уже есть активная заявка на этот автомобиль");
        }

        if (request.getStatus() == null) {
            request.setStatus(RequestStatus.NEW);
        }

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
        PurchaseRequest existing = purchaseRequestRepository.findById(request.getId());
        if (existing == null) {
            throw new EntityNotFoundException("Заявка с id=" + request.getId() + " не найдена");
        }

        validateUserExists(request.getUserId());
        Car car = validateCarForRequest(request.getCarId());

        if (request.getStatus() != existing.getStatus()) {
            validateStatusTransition(existing.getStatus(), request.getStatus());
        }

        if (!request.getUserId().equals(existing.getUserId())
                || !request.getCarId().equals(existing.getCarId())) {
            if (car.getStatus() == CarStatus.SOLD || car.getStatus() == CarStatus.ARCHIVED) {
                throw new BusinessException("Нельзя привязать заявку к автомобилю со статусом " + car.getStatus());
            }
            if (hasActiveRequest(request.getUserId(), request.getCarId(), request.getId())) {
                throw new BusinessException("У пользователя уже есть активная заявка на этот автомобиль");
            }
        }

        purchaseRequestRepository.update(request);
        return request;
    }

    public void delete(Long id) {
        if (!purchaseRequestRepository.delete(id)) {
            throw new EntityNotFoundException("Заявка с id=" + id + " не найдена");
        }
    }

    public List<PurchaseRequest> findByUserId(Long userId) {
        validateUserExists(userId);

        List<PurchaseRequest> result = new java.util.ArrayList<>();
        for (PurchaseRequest request : purchaseRequestRepository.findAll()) {
            if (request.getUserId().equals(userId)) {
                result.add(request);
            }
        }
        return result;
    }

    private void validateUserExists(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new EntityNotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private Car validateCarForRequest(Long carId) {
        Car car = carRepository.findById(carId);
        if (car == null) {
            throw new EntityNotFoundException("Автомобиль с id=" + carId + " не найден");
        }
        return car;
    }

    private boolean hasActiveRequest(Long userId, Long carId, Long excludeRequestId) {
        for (PurchaseRequest request : purchaseRequestRepository.findAll()) {
            if (excludeRequestId != null && excludeRequestId.equals(request.getId())) {
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
        return status == RequestStatus.NEW || status == RequestStatus.IN_PROGRESS;
    }

    private void validateStatusTransition(RequestStatus from, RequestStatus to) {
        if (from == to) {
            return;
        }

        boolean allowed = false;

        if (from == RequestStatus.NEW) {
            allowed = to == RequestStatus.IN_PROGRESS || to == RequestStatus.CANCELLED;
        } else if (from == RequestStatus.IN_PROGRESS) {
            allowed = to == RequestStatus.APPROVED
                    || to == RequestStatus.REJECTED
                    || to == RequestStatus.CANCELLED;
        }

        if (!allowed) {
            throw new BusinessException("Недопустимый переход статуса: " + from + " -> " + to);
        }
    }
}
