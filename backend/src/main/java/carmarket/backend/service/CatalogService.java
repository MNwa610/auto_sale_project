package carmarket.backend.service;

import carmarket.backend.domain.*;
import carmarket.backend.dto.*;
import carmarket.backend.error.ApiFailure;
import carmarket.backend.repository.CatalogRepository;
import jakarta.persistence.LockModeType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CatalogRepository repository;
    private final AuditService audit;
    public CatalogService(CatalogRepository repository, AuditService audit) { this.repository = repository; this.audit = audit; }

    public List<CatalogResponse> list(CatalogKind kind, Long brandId) {
        if (brandId != null && (kind != CatalogKind.MODELS || brandId <= 0)) {
            throw ApiFailure.invalid("brandId применим только к моделям и должен быть положительным.");
        }
        return repository.findAll(kind, brandId).stream().map(CatalogResponse::from).toList();
    }

    public CatalogResponse get(CatalogKind kind, long id) { return CatalogResponse.from(require(kind, id, LockModeType.NONE)); }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogResponse create(CatalogKind kind, CatalogWriteRequest request) {
        if (request.version() != null) throw ApiFailure.invalid("При создании version назначается сервером.");
        CatalogEntry entry = kind.newEntry();
        validateAndApply(kind, entry, request);
        repository.persist(entry);
        repository.flush();
        audit.recordAsCurrentUser(AuditAction.CREATED, AuditTarget.catalog(kind, entry.getId()), AuditDetails.none());
        return CatalogResponse.from(entry);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogResponse update(CatalogKind kind, long id, CatalogWriteRequest request) {
        CatalogEntry entry = require(kind, id, LockModeType.PESSIMISTIC_WRITE);
        checkVersion(entry.getVersion(), request.version());
        String name = entry.getName();
        boolean enabled = entry.isEnabled();
        validateAndApply(kind, entry, request);
        Set<AuditDetails.Field> fields = java.util.EnumSet.noneOf(AuditDetails.Field.class);
        if (!name.equals(entry.getName())) fields.add(AuditDetails.Field.NAME);
        if (enabled != entry.isEnabled()) fields.add(AuditDetails.Field.ENABLED);
        repository.flush();
        if (!fields.isEmpty()) audit.recordAsCurrentUser(AuditAction.UPDATED,
                AuditTarget.catalog(kind, id), AuditDetails.changed(fields));
        return CatalogResponse.from(entry);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogResponse setEnabled(CatalogKind kind, long id, CatalogEnabledRequest request) {
        CatalogEntry entry = require(kind, id, LockModeType.PESSIMISTIC_WRITE);
        checkVersion(entry.getVersion(), request.version());
        if (entry.isEnabled() != request.enabled()) {
            if (entry instanceof CarModel model && request.enabled()) {
                Brand brand = (Brand) require(CatalogKind.BRANDS, model.getBrand().getId(), LockModeType.PESSIMISTIC_READ);
                ensureEnabled(brand);
            }
            entry.setEnabled(request.enabled());
            repository.flush();
            audit.recordAsCurrentUser(AuditAction.UPDATED, AuditTarget.catalog(kind, id),
                    AuditDetails.changed(Set.of(AuditDetails.Field.ENABLED)));
        }
        return CatalogResponse.from(entry);
    }

    private void validateAndApply(CatalogKind kind, CatalogEntry entry, CatalogWriteRequest request) {
        String name = request.name().strip();
        if (name.isEmpty() || name.length() > kind.nameLimit()) throw ApiFailure.invalid("Недопустимая длина названия справочника.");
        if (kind == CatalogKind.MODELS) {
            if (request.brandId() == null) throw ApiFailure.invalid("Для модели требуется brandId.");
            CarModel model = (CarModel) entry;
            boolean existing = model.getId() != null;
            if (existing && !Objects.equals(model.getBrand().getId(), request.brandId())) {
                throw ApiFailure.invalid("Нельзя переносить существующую модель к другой марке. Создайте новую модель.");
            }
            Brand brand = (Brand) require(CatalogKind.BRANDS, request.brandId(), LockModeType.PESSIMISTIC_READ);
            if (!existing || (!model.isEnabled() && request.enabled())) ensureEnabled(brand);
            model.setBrand(brand);
        } else if (request.brandId() != null) throw ApiFailure.invalid("brandId разрешён только для моделей.");
        if (repository.nameExists(kind, name, request.brandId(), entry.getId())) {
            throw ApiFailure.conflict("DUPLICATE_RECORD", "Такое название уже есть в справочнике.");
        }
        entry.setName(name);
        entry.setEnabled(request.enabled());
    }

    /** Вызывается из пишущей транзакции CarService; read-lock исключает гонку с отключением. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public CatalogEntry forCar(CatalogKind kind, long id, Long previousId) {
        CatalogEntry entry = require(kind, id, LockModeType.PESSIMISTIC_READ);
        boolean unchanged = Objects.equals(entry.getId(), previousId);
        if (!unchanged) {
            ensureEnabled(entry);
            if (entry instanceof CarModel model) {
                ensureEnabled(require(CatalogKind.BRANDS, model.getBrand().getId(), LockModeType.PESSIMISTIC_READ));
            }
        }
        return entry;
    }

    private CatalogEntry require(CatalogKind kind, long id, LockModeType lock) {
        CatalogEntry entry = id <= 0 ? null : repository.find(kind, id, lock);
        if (entry == null) throw ApiFailure.notFound();
        return entry;
    }
    private static void ensureEnabled(CatalogEntry entry) {
        if (!entry.isEnabled()) throw ApiFailure.conflict("INACTIVE_CATALOG", "Нельзя выбрать отключённый справочник.");
    }
    public static void checkVersion(long actual, Long expected) {
        if (expected == null || expected < 0) throw ApiFailure.invalid("При изменении требуется неотрицательный version.");
        if (actual != expected) throw ApiFailure.conflict("VERSION_CONFLICT", "Запись изменена другим пользователем. Обновите данные.");
    }
}
