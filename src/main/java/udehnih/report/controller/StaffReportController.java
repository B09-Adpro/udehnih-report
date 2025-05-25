package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/reports")
@PreAuthorize("hasRole('STAFF')")
public class StaffReportController {

    private final ReportService reportService;
    
    public StaffReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<?>> getAllReports() {
        // Get the current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if the user has the STAFF role
        boolean hasStaffRole = authentication != null && 
                              authentication.getAuthorities().stream()
                                  .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        
        if (!hasStaffRole) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(403).body("Access denied: STAFF role required"));
        }
        
        return reportService.getAllReports()
            .thenApply(reports -> {
                return reports.stream()
                    .map(ReportMapper::toDto)
                    .collect(Collectors.toList());
            })
            .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<ReportResponseDto> processReport(
            @PathVariable("reportId") final Integer reportId,
            @RequestBody(required = false) final RejectionRequestDto rejectionRequest) {
        ResponseEntity<ReportResponseDto> response;
        
        try {
            final Report processed = reportService.processReport(reportId, rejectionRequest);
            response = ResponseEntity.ok(ReportMapper.toDto(processed));
        } catch (udehnih.report.exception.ReportNotFoundException e) {
            // Log the specific exception
            response = ResponseEntity.notFound().build();
        } catch (udehnih.report.exception.InvalidReportStateException e) {
            // Log the specific exception
            response = ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Log unexpected exceptions
            response = ResponseEntity.internalServerError().build();
        }
        
        return response;
    }
}
