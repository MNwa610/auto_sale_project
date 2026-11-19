package carmarket.backend.controller;

import carmarket.backend.domain.CatalogKind;
import carmarket.backend.dto.*;
import carmarket.backend.service.CatalogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogs/{kind}")
@Tag(name = "Справочники")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }
    @GetMapping public List<CatalogResponse> list(@PathVariable String kind, @RequestParam(required = false) Long brandId) {
        return service.list(CatalogKind.fromPath(kind), brandId);
    }
    @GetMapping("/{id}") public CatalogResponse get(@PathVariable String kind, @PathVariable long id) {
        return service.get(CatalogKind.fromPath(kind), id);
    }
    @PostMapping public ResponseEntity<CatalogResponse> create(@PathVariable String kind, @Valid @RequestBody CatalogWriteRequest request) {
        CatalogResponse response = service.create(CatalogKind.fromPath(kind), request);
        return ResponseEntity.created(URI.create("/api/v1/catalogs/" + kind + "/" + response.id())).body(response);
    }
    @PutMapping("/{id}") public CatalogResponse update(@PathVariable String kind, @PathVariable long id,
                                                      @Valid @RequestBody CatalogWriteRequest request) {
        return service.update(CatalogKind.fromPath(kind), id, request);
    }
    @PatchMapping("/{id}/enabled") public CatalogResponse enabled(@PathVariable String kind, @PathVariable long id,
                                                                 @Valid @RequestBody CatalogEnabledRequest request) {
        return service.setEnabled(CatalogKind.fromPath(kind), id, request);
    }
}
