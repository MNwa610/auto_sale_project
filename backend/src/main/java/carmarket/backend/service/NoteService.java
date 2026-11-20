package carmarket.backend.service;
import carmarket.backend.domain.*;
import carmarket.backend.dto.*;
import carmarket.backend.repository.CarRepository;
import carmarket.backend.error.ApiFailure;
import carmarket.backend.security.SessionUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class NoteService {
    private final JdbcTemplate jdbc; private final CarRepository cars; private final AuditService audit; private final Clock clock;
    public NoteService(JdbcTemplate jdbc, CarRepository cars, AuditService audit,Clock clock) {
        this.jdbc=jdbc;this.cars=cars;this.audit=audit;this.clock=clock;
    }
    public record Note(UUID id, UUID carId, long authorId, String authorName, String content, boolean pinned,
                       long version, Instant createdAt, Instant updatedAt) { }
    public List<Note> list(UUID carId) {
        Car car=requireCar(carId,false);
        return jdbc.query("SELECT n.*,u.full_name FROM car_notes n JOIN users u ON u.id=n.author_id WHERE n.car_id=? ORDER BY n.pinned DESC,n.created_at DESC,n.id",
            (r,i)->new Note(r.getObject("id",UUID.class),carId,r.getLong("author_id"),r.getString("full_name"),r.getString("content"),r.getBoolean("pinned"),
                r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),car.getId());
    }
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Note create(UUID carId,NoteWriteRequest request) {
        Car car=requireCar(carId,true);
        if(request.version()!=null) throw ApiFailure.invalid("При создании version назначается сервером.");
        UUID id=UUID.randomUUID(); Instant now=clock.instant();
        jdbc.update("INSERT INTO car_notes(id,car_id,author_id,content,pinned,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
            id,car.getId(),current().id(),request.content().strip(),request.pinned(),java.sql.Timestamp.from(now),java.sql.Timestamp.from(now));
        audit.recordAsCurrentUser(AuditAction.CREATED,AuditTarget.note(id),AuditDetails.none());
        return note(carId,id);
    }
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Note update(UUID carId,UUID id,NoteWriteRequest request) {
        requireCar(carId,true); Note before=note(carId,id); authorize(before);
        CatalogService.checkVersion(before.version(),request.version());
        if(!before.content().equals(request.content().strip()) || before.pinned()!=request.pinned()) {
            jdbc.update("UPDATE car_notes SET content=?,pinned=?,version=version+1,updated_at=? WHERE id=?",
                request.content().strip(),request.pinned(),java.sql.Timestamp.from(clock.instant()),id);
            audit.recordAsCurrentUser(AuditAction.UPDATED,AuditTarget.note(id),AuditDetails.none());
        }
        return note(carId,id);
    }
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public void delete(UUID carId,UUID id,Long version) {
        requireCar(carId,true); Note note=note(carId,id); authorize(note); CatalogService.checkVersion(note.version(),version);
        jdbc.update("DELETE FROM car_notes WHERE id=?",id);
        audit.recordAsCurrentUser(AuditAction.DELETED,AuditTarget.note(id),AuditDetails.none());
    }
    private Note note(UUID carId,UUID id) { return list(carId).stream().filter(n->n.id().equals(id)).findFirst().orElseThrow(ApiFailure::notFound); }
    private Car requireCar(UUID id, boolean write) {
        Car car=cars.find(id,write); if(car==null)throw ApiFailure.notFound();
        if(write && car.isArchived())throw ApiFailure.conflict("ARCHIVED_CAR","Сначала восстановите автомобиль из архива.");
        return car;
    }
    private void authorize(Note note) {
        if(current().id()!=note.authorId() && current().role()!=RoleCode.ADMIN)
            throw new ApiFailure(HttpStatus.FORBIDDEN,"FORBIDDEN","Изменять заметку может автор или администратор.");
    }
    public static SessionUser current() { return (SessionUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}
