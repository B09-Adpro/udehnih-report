package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/reports")
@PreAuthorize("hasRole('STAFF')")
public class StaffReportController {

    private final ReportService reportService;
    
    @Autowired
    @Qualifier("authJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    
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
                // Get all unique student IDs from the reports
                List<String> studentIds = reports.stream()
                    .map(Report::getStudentId)
                    .distinct()
                    .collect(Collectors.toList());
                
                // Create a map of student IDs to student names
                Map<String, String> studentNames = new HashMap<>();
                
                // Fetch student names from the auth database
                if (!studentIds.isEmpty()) {
                    for (String studentId : studentIds) {
                        try {
                            // Try to parse the studentId as an integer
                            Integer id;
                            try {
                                id = Integer.parseInt(studentId);
                            } catch (NumberFormatException e) {
                                // If studentId is not a valid integer, use it as is
                                id = null;
                            }
                            
                            String name;
                            if (id != null) {
                                // Query by ID if it's a valid integer
                                String sql = "SELECT name FROM users WHERE id = ?";
                                name = jdbcTemplate.queryForObject(sql, String.class, id);
                            } else {
                                // If studentId is not a valid integer, try to query by the ID as a string
                                String sql = "SELECT name FROM users WHERE id::text = ? OR email = ?";
                                name = jdbcTemplate.queryForObject(sql, String.class, studentId, studentId);
                            }
                            
                            studentNames.put(studentId, name != null ? name : "Unknown");
                        } catch (Exception e) {
                            // Log the error for debugging
                            System.out.println("Error fetching student name for ID " + studentId + ": " + e.getMessage());
                            studentNames.put(studentId, "Unknown");
                        }
                    }
                }
                
                // Map reports to DTOs and set student names
                List<ReportResponseDto> reportDtos = reports.stream()
                    .map(report -> {
                        ReportResponseDto dto = ReportMapper.toDto(report);
                        dto.setStudentName(studentNames.getOrDefault(report.getStudentId(), "Unknown"));
                        return dto;
                    })
                    .collect(Collectors.toList());
                
                return reportDtos;
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
