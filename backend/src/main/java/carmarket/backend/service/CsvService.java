package carmarket.backend.service;
import carmarket.backend.domain.*;
import carmarket.backend.dto.*;
import carmarket.backend.repository.CarRepository;
import carmarket.backend.error.ApiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

@Service
public class CsvService {
    public enum Mode { NEW_ONLY, UPDATE_BY_EXTERNAL_CODE }
    public record CheckedRow(int line,CarWriteRequest data,UUID carId,String error) { }
    public record Preview(UUID id,Mode mode,int validRows,int invalidRows,Instant expiresAt,List<CheckedRow> rows) { }
    public record Applied(UUID id,int created,int updated) { }
    private final CarCsvCodec codec; private final CarService cars; private final CarRepository repository;
    private final JdbcTemplate jdbc; private final ObjectMapper json; private final Validator validator;
    private final TransactionTemplate tx; private final Clock clock; private final AuditService audit;
    public CsvService(CarCsvCodec codec,CarService cars,CarRepository repository,JdbcTemplate jdbc,ObjectMapper json,
                      Validator validator,PlatformTransactionManager manager,Clock clock,AuditService audit) {
        this.codec=codec;this.cars=cars;this.repository=repository;this.jdbc=jdbc;this.json=json;this.validator=validator;
        tx=new TransactionTemplate(manager); tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock=clock;this.audit=audit;
    }
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Preview preview(String source,Mode mode) {
        if(mode==null)throw ApiFailure.invalid("Укажите режим импорта.");
        if(source.getBytes(StandardCharsets.UTF_8).length>2*1024*1024)throw ApiFailure.invalid("CSV ограничен 2 MiB.");
        var parsed=codec.parse(source); List<CheckedRow> rows=new ArrayList<>();
        Set<String> codes=new HashSet<>(), vins=new HashSet<>();
        for(var row:parsed) {
            if(row.error()!=null){rows.add(new CheckedRow(row.line(),null,null,row.error()));continue;}
            var data=row.data(); String code=data.externalCode().strip();
            String vin=data.vin()==null?"":data.vin().strip().toUpperCase(Locale.ROOT);
            if(!codes.add(code)||!vin.isEmpty()&&!vins.add(vin)) { rows.add(new CheckedRow(row.line(),null,null,"DUPLICATE_IN_FILE"));continue; }
            if(!validator.validate(data).isEmpty()) { rows.add(new CheckedRow(row.line(),null,null,"INVALID_DATA"));continue; }
            try {
                CheckedRow checked=tx.execute(status->{
                    status.setRollbackOnly();
                    Car existing=repository.byExternalCode(code);
                    if(existing!=null && mode==Mode.NEW_ONLY)throw ApiFailure.conflict("DUPLICATE_RECORD","Внешний код уже существует.");
                    if(existing==null){cars.create(data);return new CheckedRow(row.line(),data,null,null);}
                    var versioned=version(data,existing.getVersion());
                    cars.update(existing.getPublicId(),versioned);
                    return new CheckedRow(row.line(),versioned,existing.getPublicId(),null);
                });
                rows.add(checked);
            }catch(ApiFailure e){rows.add(new CheckedRow(row.line(),null,null,e.getCode()));}
            catch(org.springframework.dao.DataIntegrityViolationException e){rows.add(new CheckedRow(row.line(),null,null,"DUPLICATE_OR_RELATED_RECORD"));}
        }
        UUID id=UUID.randomUUID(); Instant now=clock.instant(), expires=now.plus(Duration.ofMinutes(30));
        int valid=(int)rows.stream().filter(r->r.error()==null).count();
        Preview preview=new Preview(id,mode,valid,rows.size()-valid,expires,List.copyOf(rows));
        tx.executeWithoutResult(status->jdbc.update("INSERT INTO csv_import_sessions(id,user_id,mode,file_hash,valid_rows,invalid_rows,payload,errors,created_at,expires_at) VALUES (?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?,?)",
            id,NoteService.current().id(),mode.name(),hash(source),valid,rows.size()-valid,encode(preview),encode(rows.stream().filter(r->r.error()!=null).toList()),java.sql.Timestamp.from(now),java.sql.Timestamp.from(expires)));
        return preview;
    }
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Applied apply(UUID id) {
        var sessions=jdbc.queryForList("SELECT payload::text,expires_at,applied_at FROM csv_import_sessions WHERE id=? AND user_id=? FOR UPDATE",id,NoteService.current().id());
        if(sessions.isEmpty())throw ApiFailure.notFound(); var session=sessions.get(0);
        if(session.get("applied_at")!=null)throw ApiFailure.conflict("IMPORT_ALREADY_APPLIED","Импорт уже применён.");
        if(!((java.sql.Timestamp)session.get("expires_at")).toInstant().isAfter(clock.instant()))throw ApiFailure.conflict("IMPORT_EXPIRED","Проверка импорта истекла.");
        Preview preview=decode((String)session.get("payload"));
        if(preview.invalidRows()>0)throw ApiFailure.conflict("IMPORT_HAS_ERRORS","Исправьте CSV и выполните проверку заново.");
        int created=0,updated=0;
        for(var row:preview.rows()) {
            CarResponse result;
            if(row.carId()==null){result=cars.create(row.data());created++;}
            else {result=cars.update(row.carId(),row.data());updated++;}
            audit.recordAsCurrentUser(AuditAction.IMPORTED,AuditTarget.car(result.id()),AuditDetails.none());
        }
        jdbc.update("UPDATE csv_import_sessions SET applied_at=? WHERE id=?",java.sql.Timestamp.from(clock.instant()),id);
        return new Applied(id,created,updated);
    }
    private CarWriteRequest version(CarWriteRequest r,long version) {
        return new CarWriteRequest(r.externalCode(),r.sellerId(),r.modelId(),r.year(),r.mileage(),r.price(),r.vin(),r.bodyTypeId(),r.transmissionId(),r.fuelTypeId(),r.engineVolume(),r.description(),r.status(),r.certified(),r.tags(),version);
    }
    private String hash(String source) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private Preview decode(String value){try{return json.readValue(value,Preview.class);}catch(Exception e){throw new IllegalStateException(e);}}
}
