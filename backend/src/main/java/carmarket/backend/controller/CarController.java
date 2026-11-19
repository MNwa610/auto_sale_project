package carmarket.backend.controller;

import carmarket.backend.dto.*;
import carmarket.backend.service.CarService;
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
    public CarController(CarService service) { this.service = service; }
    @GetMapping public List<CarResponse> list() { return service.list(); }
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
