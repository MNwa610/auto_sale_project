package carmarket.backend.repository;

import carmarket.backend.domain.*;
import carmarket.backend.dto.SellerResponse;
import carmarket.backend.dto.CarQuery;
import jakarta.persistence.criteria.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
public class CarRepository {
    @PersistenceContext private EntityManager em;

    public List<Car> findAll() {
        return em.createQuery("select c from Car c order by c.id", Car.class).getResultList();
    }
    public List<Car> search(CarQuery filter, int year, int offset, int limit) {
        var cb = em.getCriteriaBuilder(); var query = cb.createQuery(Car.class); var c = query.from(Car.class);
        query.select(c).where(predicates(filter, year, cb, c));
        var field = c.get(filter.sortField());
        query.orderBy(filter.descending() ? cb.desc(field) : cb.asc(field), cb.asc(c.get("id")));
        return em.createQuery(query).setFirstResult(offset).setMaxResults(limit).getResultList();
    }
    public long count(CarQuery filter, int year) {
        var cb = em.getCriteriaBuilder(); var query = cb.createQuery(Long.class); var c = query.from(Car.class);
        query.select(cb.count(c)).where(predicates(filter, year, cb, c));
        return em.createQuery(query).getSingleResult();
    }
    private Predicate[] predicates(CarQuery f, int year, CriteriaBuilder cb, Root<Car> c) {
        List<Predicate> p = new ArrayList<>();
        var spec = c.join("specification", JoinType.LEFT); var model = c.join("model");
        p.add(cb.equal(c.get("archived"), Boolean.TRUE.equals(f.archived())));
        eq(p,cb,model.get("brand").get("id"),f.brandId()); eq(p,cb,model.get("id"),f.modelId());
        eq(p,cb,c.get("seller").get("id"),f.sellerId()); eq(p,cb,spec.get("bodyType").get("id"),f.bodyTypeId());
        eq(p,cb,spec.get("transmission").get("id"),f.transmissionId()); eq(p,cb,spec.get("fuelType").get("id"),f.fuelTypeId());
        eq(p,cb,c.get("status"),f.status()); eq(p,cb,c.get("certified"),f.certified());
        bounds(p,cb,c.<Integer>get("year"),f.yearMin(),f.yearMax());
        bounds(p,cb,c.<Integer>get("mileage"),f.mileageMin(),f.mileageMax());
        bounds(p,cb,c.<BigDecimal>get("price"),f.priceMin(),f.priceMax());
        bounds(p,cb,spec.<BigDecimal>get("engineVolume"),f.engineVolumeMin(),f.engineVolumeMax());
        Expression<Integer> age = cb.<Integer>selectCase().when(cb.greaterThan(c.get("year"), year),0).otherwise(cb.diff(year,c.get("year")));
        bounds(p,cb,age,f.ageMin(),f.ageMax());
        if (f.tag()!=null && !f.tag().isBlank()) p.add(cb.isMember(f.tag().strip().toLowerCase(Locale.ROOT), c.get("tags")));
        if (f.q()!=null && !f.q().isBlank()) {
            String q = "%"+f.q().strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            p.add(cb.or(cb.like(cb.lower(c.get("externalCode")),q,'!'), cb.like(cb.lower(spec.get("vin")),q,'!'),
                cb.like(cb.lower(spec.get("description")),q,'!'),cb.like(cb.lower(model.get("name")),q,'!'),
                cb.like(cb.lower(model.get("brand").get("name")),q,'!')));
        }
        return p.toArray(Predicate[]::new);
    }
    private static void eq(List<Predicate> p, CriteriaBuilder cb, Expression<?> field, Object value) {
        if(value!=null) p.add(cb.equal(field,value));
    }
    private static <T extends Comparable<? super T>> void bounds(List<Predicate> p, CriteriaBuilder cb, Expression<T> field,T min,T max) {
        if(min!=null)p.add(cb.greaterThanOrEqualTo(field,min)); if(max!=null)p.add(cb.lessThanOrEqualTo(field,max));
    }
    public Car byExternalCode(String code) {
        // Read the preview snapshot only after acquiring the same lock as normal updates.
        List<?> ids = em.createNativeQuery("SELECT id FROM cars WHERE external_code=:code FOR UPDATE")
            .setParameter("code",code).getResultList();
        return ids.isEmpty() ? null : em.find(Car.class,((Number)ids.getFirst()).longValue());
    }
    public void remove(Car car) { em.remove(car); }
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
