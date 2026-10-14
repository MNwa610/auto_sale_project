package carmarket.service;

import carmarket.model.PurchaseRequest;
import carmarket.repository.PurchaseRequestRepository;

import java.util.ArrayList;
import java.util.List;

public class InMemoryPurchaseRequestRepository extends PurchaseRequestRepository {

    private final List<PurchaseRequest> requests = new ArrayList<>();
    private long nextId = 1;

    public void seed(PurchaseRequest request) {
        if (request.getId() == null) {
            request.setId(nextId++);
        }
        requests.add(request);
    }

    @Override
    public PurchaseRequest create(PurchaseRequest request) {
        request.setId(nextId++);
        requests.add(request);
        return request;
    }

    @Override
    public List<PurchaseRequest> findAll() {
        return new ArrayList<>(requests);
    }

    @Override
    public List<PurchaseRequest> findAllWithDetails() {
        return findAll();
    }

    @Override
    public List<PurchaseRequest> findByUserId(Long userId) {
        List<PurchaseRequest> result = new ArrayList<>();
        for (PurchaseRequest request : requests) {
            if (request.getUserId().equals(userId)) {
                result.add(request);
            }
        }
        return result;
    }

    @Override
    public List<PurchaseRequest> findByCarId(Long carId) {
        List<PurchaseRequest> result = new ArrayList<>();
        for (PurchaseRequest request : requests) {
            if (request.getCarId().equals(carId)) {
                result.add(request);
            }
        }
        return result;
    }

    @Override
    public PurchaseRequest findById(Long id) {
        for (PurchaseRequest request : requests) {
            if (request.getId().equals(id)) {
                return request;
            }
        }
        return null;
    }

    @Override
    public boolean update(PurchaseRequest request) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).getId().equals(request.getId())) {
                requests.set(i, request);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        return requests.removeIf(request -> request.getId().equals(id));
    }
}
