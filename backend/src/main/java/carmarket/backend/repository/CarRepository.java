package carmarket.backend.repository;

import carmarket.backend.domain.*;
import carmarket.backend.dto.SellerResponse;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class CarRepository {
    @PersistenceContext private EntityManager em;

    public List<Car> findAll() {
        return em.createQuery("select c from Car c order by c.id", Car.class).getResultList();
    }

    public Car find(UUID publicId, boolean forUpdate) {
        if (forUpdate) {
            // Блокируем только cars, без FOR UPDATE на nullable JOIN характеристик.
            List<?> ids = em.createNativeQuery("SELECT id FROM cars WHERE public_id=:id FOR UPDATE")
                    .setParameter("id", publicId).getResultList();
            return ids.isEmpty() ? null : em.find(Car.class, ((Number) ids.get(0)).longValue());
        }
        return em.createQuery("select c from Car c where c.publicId=:id", Car.class)
                .setParameter("id", publicId).getResultList().stream().findFirst().orElse(null);
    }

    public UserAccount seller(long id) { return em.find(UserAccount.class, id, LockModeType.PESSIMISTIC_READ); }

    public List<SellerResponse> sellers() {
        return em.createQuery("select new carmarket.backend.dto.SellerResponse(u.id, u.fullName) "
                + "from UserAccount u where u.enabled=true order by u.fullName, u.id", SellerResponse.class).getResultList();
    }

    public boolean externalCodeExists(String code, Long exceptId) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Car.class);
        var condition = cb.equal(root.get("externalCode"), code);
        if (exceptId != null) condition = cb.and(condition, cb.notEqual(root.get("id"), exceptId));
        query.select(cb.count(root)).where(condition);
        return em.createQuery(query).getSingleResult() > 0;
    }

    public boolean vinExists(String vin, Long exceptId) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(CarSpecification.class);
        var condition = cb.equal(root.get("vin"), vin);
        if (exceptId != null) condition = cb.and(condition, cb.notEqual(root.get("car").get("id"), exceptId));
        query.select(cb.count(root)).where(condition);
        return em.createQuery(query).getSingleResult() > 0;
    }

    public void persist(Car car) { em.persist(car); }
    public void flush() { em.flush(); }
}
