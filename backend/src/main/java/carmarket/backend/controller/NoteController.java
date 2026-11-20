package carmarket.backend.controller;
import carmarket.backend.dto.*;
import carmarket.backend.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.*;
@RestController
@RequestMapping("/api/v1/cars/{carId}/notes")
@SecurityRequirement(name="bearerAuth")
public class NoteController {
    private final NoteService notes;
    public NoteController(NoteService notes) { this.notes=notes; }
    @GetMapping public List<NoteService.Note> list(@PathVariable UUID carId) { return notes.list(carId); }
    @PostMapping public ResponseEntity<NoteService.Note> create(@PathVariable UUID carId,@Valid @RequestBody NoteWriteRequest request) {
        var note=notes.create(carId,request); return ResponseEntity.created(URI.create("/api/v1/cars/"+carId+"/notes/"+note.id())).body(note);
    }
    @PutMapping("/{id}") public NoteService.Note update(@PathVariable UUID carId,@PathVariable UUID id,@Valid @RequestBody NoteWriteRequest request) {
        return notes.update(carId,id,request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID carId,@PathVariable UUID id,@RequestParam Long version) {
        notes.delete(carId,id,version);return ResponseEntity.noContent().build();
    }
}
