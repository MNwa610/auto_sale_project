package carmarket.backend.service;
import carmarket.backend.dto.*;
import carmarket.backend.error.ApiFailure;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import java.util.*;
@Service
public class BulkCarService {
    private final CarService cars;
    private final TransactionTemplate tx;
    public BulkCarService(CarService cars, PlatformTransactionManager manager) {
        this.cars=cars; tx=new TransactionTemplate(manager);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public record Result(UUID id,int status,String code,CarResponse car) { }
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') && (#request.action().name() != 'DELETE' || hasRole('ADMIN'))")
    public List<Result> apply(BulkRequest request) {
        List<Result> results=new ArrayList<>(); Set<UUID> seen=new HashSet<>();
        for(var item:request.items()) {
            if(!seen.add(item.id())) { results.add(new Result(item.id(),400,"DUPLICATE_ITEM",null)); continue; }
            try {
                CarResponse car=tx.execute(s -> {
                    if(request.action()==BulkRequest.Action.DELETE) { cars.delete(item.id(),item.version()); return null; }
                    return cars.archive(item.id(),item.version(),request.action()==BulkRequest.Action.ARCHIVE);
                });
                results.add(new Result(item.id(),200,"OK",car));
            } catch(ApiFailure e) { results.add(new Result(item.id(),e.getStatus().value(),e.getCode(),null)); }
            catch(DataIntegrityViolationException e) { results.add(new Result(item.id(),409,"DUPLICATE_OR_RELATED_RECORD",null)); }
            catch(OptimisticLockingFailureException e) { results.add(new Result(item.id(),409,"VERSION_CONFLICT",null)); }
            catch(PessimisticLockingFailureException e) { results.add(new Result(item.id(),409,"CONCURRENT_CHANGE",null)); }
        }
        return results;
    }
}
