package carmarket.backend.controller;
import carmarket.backend.dto.CarQuery;
import carmarket.backend.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/cars/csv")
@SecurityRequirement(name="bearerAuth")
public class CsvController {
    private final CsvService csv; private final CarService cars; private final CarCsvCodec codec;
    public CsvController(CsvService csv,CarService cars,CarCsvCodec codec) {this.csv=csv;this.cars=cars;this.codec=codec;}
    @GetMapping(value="/export",produces="text/csv")
    public ResponseEntity<byte[]> export(@ModelAttribute CarQuery query) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=cars.csv")
            .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).cacheControl(CacheControl.noStore())
            .body(codec.export(cars.export(query)).getBytes(StandardCharsets.UTF_8));
    }
    @PostMapping(value="/preview",consumes="text/csv")
    public CsvService.Preview preview(@RequestBody byte[] source,@RequestParam CsvService.Mode mode) {
        try {
            String text=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(source)).toString();
            return csv.preview(text,mode);
        } catch(java.nio.charset.CharacterCodingException e) { throw carmarket.backend.error.ApiFailure.invalid("CSV должен быть в UTF-8."); }
    }
    @PostMapping("/{id}/apply") public CsvService.Applied apply(@PathVariable UUID id) {return csv.apply(id);}
}
