package carmarket.backend.dto;
import carmarket.backend.domain.CarStatus;
import carmarket.backend.error.ApiFailure;
import java.math.BigDecimal;

/** Inclusive ranges; all filters combine with AND. */
public record CarQuery(String q, Long brandId, Long modelId, Long sellerId, Long bodyTypeId,
        Long transmissionId, Long fuelTypeId, CarStatus status, Boolean certified, Boolean archived,
        String tag, Integer yearMin, Integer yearMax, Integer mileageMin, Integer mileageMax,
        BigDecimal priceMin, BigDecimal priceMax, Integer ageMin, Integer ageMax,
        BigDecimal engineVolumeMin, BigDecimal engineVolumeMax, Integer page, Integer size, String sort) {
    public int pageNumber() { return page == null ? 0 : page; }
    public int pageSize() { return size == null ? 20 : size; }
    public String sortField() { return sort == null ? "createdAt" : sort.split(",", -1)[0]; }
    public boolean descending() { return sort == null || sort.endsWith(",desc"); }
    public void validate() {
        if (pageNumber() < 0 || pageSize() < 1 || pageSize() > 100 || (long)pageNumber()*pageSize() > Integer.MAX_VALUE)
            throw ApiFailure.invalid("page >= 0; size: 1–100; смещение не более 2147483647.");
        if (sort != null && !sort.matches("(price|year|mileage|createdAt|updatedAt|externalCode),(asc|desc)"))
            throw ApiFailure.invalid("sort: price, year, mileage, createdAt, updatedAt, externalCode + ,asc или ,desc.");
        if (q != null && q.length() > 200 || tag != null && tag.length() > 50) throw ApiFailure.invalid("Слишком длинный фильтр.");
        range(yearMin,yearMax); range(mileageMin,mileageMax); range(priceMin,priceMax); range(ageMin,ageMax);
        range(engineVolumeMin,engineVolumeMax);
        for (Long id : new Long[]{brandId,modelId,sellerId,bodyTypeId,transmissionId,fuelTypeId})
            if (id != null && id < 1) throw ApiFailure.invalid("ID фильтра должен быть положительным.");
    }
    private static <T extends Number & Comparable<T>> void range(T min,T max) {
        if (min != null && min.doubleValue()<0 || max != null && max.doubleValue()<0 || min!=null && max!=null && min.compareTo(max)>0)
            throw ApiFailure.invalid("Некорректный числовой диапазон.");
    }
}
