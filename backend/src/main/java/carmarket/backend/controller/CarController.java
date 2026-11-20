package carmarket.backend.controller;

import carmarket.backend.dto.*;
import carmarket.backend.service.CarService;
import carmarket.backend.service.BulkCarService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
@Tag(name = "Автомобили")
@SecurityRequirement(name = "bearerAuth")
public class CarController {
    private final CarService service;
    private final BulkCarService bulk;
    public CarController(CarService service, BulkCarService bulk) { this.service = service; this.bulk=bulk; }
    @GetMapping public PageResponse<CarResponse> list(@ModelAttribute CarQuery query) { return service.list(query); }
    @PostMapping("/{id}/archive") public CarResponse archive(@PathVariable UUID id,@Valid @RequestBody VersionRequest request) {
        return service.archive(id,request.version(),true);
    }
    @PostMapping("/{id}/restore") public CarResponse restore(@PathVariable UUID id,@Valid @RequestBody VersionRequest request) {
        return service.archive(id,request.version(),false);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id,@RequestParam Long version) {
        service.delete(id,version); return ResponseEntity.noContent().build();
    }
    @PostMapping("/bulk") public List<BulkCarService.Result> bulk(@Valid @RequestBody BulkRequest request) { return bulk.apply(request); }
    @GetMapping("/sellers") public List<SellerResponse> sellers() { return service.sellers(); }
    @GetMapping("/{id}") public CarResponse get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping public ResponseEntity<CarResponse> create(@Valid @RequestBody CarWriteRequest request) {
        CarResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/cars/" + response.id())).body(response);
    }
    @PutMapping("/{id}") public CarResponse update(@PathVariable UUID id, @Valid @RequestBody CarWriteRequest request) {
        return service.update(id, request);
    }
}
