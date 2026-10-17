package carmarket.service;

import carmarket.model.Brand;
import carmarket.model.CarModel;
import carmarket.model.NamedCatalogItem;
import carmarket.repository.CatalogRepository;

import java.util.List;

public class CatalogService {
    private final CatalogRepository repository = new CatalogRepository();

    public List<Brand> findBrands() {
        return repository.findAllBrands();
    }

    public List<CarModel> findModelsByBrand(Long brandId) {
        return repository.findAllModels().stream()
                .filter(model -> model.getBrandId().equals(brandId)).toList();
    }

    public List<NamedCatalogItem> findBodyTypes() {
        return repository.findAllBodyTypes();
    }

    public List<NamedCatalogItem> findTransmissions() {
        return repository.findAllTransmissions();
    }

    public List<NamedCatalogItem> findFuelTypes() {
        return repository.findAllFuelTypes();
    }
}
