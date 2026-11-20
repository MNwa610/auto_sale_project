package carmarket.backend.service;

import carmarket.backend.domain.*;
import carmarket.backend.dto.*;
import carmarket.backend.error.ApiFailure;
import carmarket.backend.repository.CarRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CarService {
    private final CarRepository repository;
    private final CatalogService catalogs;
    private final CarAgeCalculator age;
    private final AuditService audit;
    public CarService(CarRepository repository, CatalogService catalogs, CarAgeCalculator age, AuditService audit) {
        this.repository = repository; this.catalogs = catalogs; this.age = age; this.audit = audit;
    }

    public PageResponse<CarResponse> list(CarQuery query) {
        query.validate(); long total = repository.count(query, age.currentYear());
        return new PageResponse<>(repository.search(query, age.currentYear(), query.pageNumber()*query.pageSize(), query.pageSize())
            .stream().map(this::response).toList(),query.pageNumber(),query.pageSize(),total,(total+query.pageSize()-1)/query.pageSize());
    }
    public List<CarResponse> export(CarQuery query) {
        query.validate();
        if(repository.count(query,age.currentYear())>10000) throw ApiFailure.invalid("Экспорт ограничен 10000 строк. Уточните фильтры.");
        return repository.search(query,age.currentYear(),0,10000).stream().map(this::response).toList();
    }
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public CarResponse archive(UUID id, Long version, boolean archived) {
        Car car=require(id,true); CatalogService.checkVersion(car.getVersion(),version);
        if(car.isArchived()!=archived) {
            car.setArchived(archived); repository.flush();
            audit.recordAsCurrentUser(archived?AuditAction.ARCHIVED:AuditAction.RESTORED,AuditTarget.car(id),AuditDetails.none());
        }
        return response(car);
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id, Long version) {
        Car car=require(id,true); CatalogService.checkVersion(car.getVersion(),version);
        if(!car.isArchived()) throw ApiFailure.conflict("NOT_ARCHIVED","Удаление возможно только из архива.");
        repository.remove(car); repository.flush();
        audit.recordAsCurrentUser(AuditAction.DELETED,AuditTarget.car(id),AuditDetails.none());
    }
    public CarResponse get(UUID id) { return response(require(id, false)); }
    public List<SellerResponse> sellers() { return repository.sellers(); }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public CarResponse create(CarWriteRequest request) {
        if (request.version() != null) throw ApiFailure.invalid("При создании version назначается сервером.");
        Car car = new Car();
        apply(car, request);
        repository.persist(car);
        repository.flush();
        audit.recordAsCurrentUser(AuditAction.CREATED, AuditTarget.car(car.getPublicId()), AuditDetails.none());
        return response(car);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public CarResponse update(UUID id, CarWriteRequest request) {
        Car car = require(id, true);
        CatalogService.checkVersion(car.getVersion(), request.version());
        if (car.isArchived()) throw ApiFailure.conflict("ARCHIVED_CAR", "Сначала восстановите автомобиль из архива.");
        Set<AuditDetails.Field> fields = apply(car, request);
        if (!fields.isEmpty()) {
            car.touch(); // Включает изменения только характеристик/тегов в @Version автомобиля.
            repository.flush();
            audit.recordAsCurrentUser(AuditAction.UPDATED, AuditTarget.car(id), AuditDetails.changed(fields));
        }
        return response(car);
    }

    private Set<AuditDetails.Field> apply(Car car, CarWriteRequest request) {
        if (request.year() > Math.min(age.currentYear() + 1, 2100)) throw ApiFailure.invalid("Год не может быть позже следующего модельного года.");
        String code = request.externalCode().strip();
        String vin = request.vin() == null || request.vin().isBlank() ? null : request.vin().strip().toUpperCase(Locale.ROOT);
        if (vin != null && !vin.matches("[A-Z0-9]{17}")) throw ApiFailure.invalid("VIN должен содержать 17 латинских букв или цифр либо отсутствовать.");
        if (repository.externalCodeExists(code, car.getId())) throw ApiFailure.conflict("DUPLICATE_RECORD", "Внешний код уже используется.");
        if (vin != null && repository.vinExists(vin, car.getId())) throw ApiFailure.conflict("DUPLICATE_RECORD", "VIN уже используется.");
        UserAccount seller = repository.seller(request.sellerId());
        if (seller == null) throw ApiFailure.notFound();
        if (!seller.isEnabled() && !Objects.equals(id(car.getSeller()), seller.getId())) {
            throw ApiFailure.conflict("INACTIVE_SELLER", "Нельзя назначить отключённого продавца.");
        }
        CarSpecification spec = car.getSpecification();
        CarModel model = (CarModel) catalogs.forCar(CatalogKind.MODELS, request.modelId(), id(car.getModel()));
        BodyType body = (BodyType) catalogs.forCar(CatalogKind.BODY_TYPES, request.bodyTypeId(), spec == null ? null : id(spec.getBodyType()));
        Transmission transmission = (Transmission) catalogs.forCar(CatalogKind.TRANSMISSIONS, request.transmissionId(), spec == null ? null : id(spec.getTransmission()));
        FuelType fuel = (FuelType) catalogs.forCar(CatalogKind.FUEL_TYPES, request.fuelTypeId(), spec == null ? null : id(spec.getFuelType()));
        if (spec == null) { spec = new CarSpecification(); spec.setCar(car); }
        String description = request.description() == null ? null : request.description().strip();
        Set<String> tags = new TreeSet<>();
        request.tags().forEach(tag -> tags.add(tag.strip().toLowerCase(Locale.ROOT)));
        Set<AuditDetails.Field> fields = EnumSet.noneOf(AuditDetails.Field.class);
        changed(fields, AuditDetails.Field.EXTERNAL_CODE, car.getExternalCode(), code);
        changed(fields, AuditDetails.Field.SELLER, id(car.getSeller()), seller.getId());
        changed(fields, AuditDetails.Field.MODEL, id(car.getModel()), model.getId());
        changed(fields, AuditDetails.Field.YEAR, car.getYear(), request.year());
        changed(fields, AuditDetails.Field.MILEAGE, car.getMileage(), request.mileage());
        changed(fields, AuditDetails.Field.PRICE, car.getPrice(), request.price().setScale(2));
        changed(fields, AuditDetails.Field.STATUS, car.getStatus(), request.status());
        changed(fields, AuditDetails.Field.CERTIFIED, car.isCertified(), request.certified());
        changed(fields, AuditDetails.Field.TAGS, car.getTags(), tags);
        changed(fields, AuditDetails.Field.VIN, spec.getVin(), vin);
        changed(fields, AuditDetails.Field.BODY_TYPE, id(spec.getBodyType()), body.getId());
        changed(fields, AuditDetails.Field.TRANSMISSION, id(spec.getTransmission()), transmission.getId());
        changed(fields, AuditDetails.Field.FUEL_TYPE, id(spec.getFuelType()), fuel.getId());
        changed(fields, AuditDetails.Field.ENGINE_VOLUME, spec.getEngineVolume(), request.engineVolume().setScale(1));
        changed(fields, AuditDetails.Field.DESCRIPTION, spec.getDescription(), description);
        car.setExternalCode(code); car.setSeller(seller); car.setModel(model); car.setYear(request.year());
        car.setMileage(request.mileage()); car.setPrice(request.price().setScale(2)); car.setStatus(request.status());
        car.setCertified(request.certified());
        if (fields.contains(AuditDetails.Field.TAGS)) { car.getTags().clear(); car.getTags().addAll(tags); }
        spec.setVin(vin); spec.setBodyType(body); spec.setTransmission(transmission); spec.setFuelType(fuel);
        spec.setEngineVolume(request.engineVolume().setScale(1)); spec.setDescription(description);
        car.setSpecification(spec);
        return fields;
    }

    private Car require(UUID id, boolean forUpdate) {
        Car car = repository.find(id, forUpdate);
        if (car == null) throw ApiFailure.notFound();
        return car;
    }
    private CarResponse response(Car car) {
        CarSpecification spec = car.getSpecification();
        return new CarResponse(car.getPublicId(), car.getExternalCode(),
                new SellerResponse(car.getSeller().getId(), car.getSeller().getFullName()),
                CatalogRef.from(car.getModel().getBrand()), CatalogRef.from(car.getModel()),
                car.getYear(), car.getMileage(), car.getPrice(), spec == null ? null : spec.getVin(),
                spec == null ? null : CatalogRef.from(spec.getBodyType()), spec == null ? null : CatalogRef.from(spec.getTransmission()),
                spec == null ? null : CatalogRef.from(spec.getFuelType()), spec == null ? null : spec.getEngineVolume(),
                spec == null ? null : spec.getDescription(), car.getStatus(), car.isCertified(),
                Collections.unmodifiableSet(new TreeSet<>(car.getTags())), age.ageYears(car.getYear()),
                car.isArchived(), car.getVersion(), car.getCreatedAt(), car.getUpdatedAt());
    }
    private static Long id(BaseEntity entity) { return entity == null ? null : entity.getId(); }
    private static void changed(Set<AuditDetails.Field> fields, AuditDetails.Field field, Object before, Object after) {
        if (!Objects.equals(before, after)) fields.add(field);
    }
}
