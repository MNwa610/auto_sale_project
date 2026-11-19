package carmarket.backend.repository;

import carmarket.backend.domain.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CatalogRepository {
    @PersistenceContext private EntityManager em;

    public List<? extends CatalogEntry> findAll(CatalogKind kind, Long brandId) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(kind.entityClass());
        var root = query.from(kind.entityClass());
        if (brandId != null) query.where(cb.equal(root.get("brand").get("id"), brandId));
        query.orderBy(cb.asc(root.get("name")), cb.asc(root.get("id")));
        return em.createQuery(query).getResultList();
    }

    public CatalogEntry find(CatalogKind kind, long id, LockModeType lock) {
        return em.find(kind.entityClass(), id, lock);
    }

    public boolean nameExists(CatalogKind kind, String name, Long brandId, Long exceptId) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(kind.entityClass());
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(cb.equal(cb.lower(cb.trim(root.get("name"))), cb.lower(cb.literal(name))));
        if (kind == CatalogKind.MODELS) conditions.add(cb.equal(root.get("brand").get("id"), brandId));
        if (exceptId != null) conditions.add(cb.notEqual(root.get("id"), exceptId));
        query.select(cb.count(root)).where(conditions.toArray(Predicate[]::new));
        return em.createQuery(query).getSingleResult() > 0;
    }

    public void persist(CatalogEntry entry) { em.persist(entry); }
    public void flush() { em.flush(); }
}
