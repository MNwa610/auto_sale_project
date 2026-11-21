package carmarket.backend.controller;

import carmarket.backend.domain.AuditAction;
import carmarket.backend.domain.AuditTarget;
import carmarket.backend.dto.PageResponse;
import carmarket.backend.error.ApiFailure;
import carmarket.backend.service.ReportingService;
import carmarket.backend.service.ReportingService.AuditEvent;
import carmarket.backend.service.ReportingService.Summary;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class ReportingController {
    private final ReportingService reports;

    public ReportingController(ReportingService reports) {
        this.reports = reports;
    }

    @GetMapping("/analytics")
    public Summary summary(@RequestParam(defaultValue = "false") boolean archived) {
        return reports.summary(archived);
    }

    @GetMapping("/audit")
    public PageResponse<AuditEvent> audit(@RequestParam(required = false) @Size(max = 50) String actorLogin,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditTarget.Type entityType,
            @RequestParam(required = false) @Size(max = 64) String entityId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiFailure.invalid("Начало периода должно быть раньше окончания.");
        }
        return reports.audit(actorLogin, action, entityType, entityId, from, to, page, size, sort);
    }
}
