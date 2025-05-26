package udehnih.report.controller;
import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import udehnih.report.client.AuthServiceClient;
import udehnih.report.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/staff/reports")
@PreAuthorize("hasRole('STAFF')")
@Slf4j
public class StaffReportController {
    private static final String UNKNOWN_USER = "Unknown";
    private final ReportService reportService;
    @Autowired
    private AuthServiceClient authServiceClient;

    public StaffReportController(ReportService reportService) {
        this.reportService = reportService;
    }
    @GetMapping
    public CompletableFuture<ResponseEntity<?>> getAllReports() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(401).body("Authentication required"));
        }
        return reportService.getAllReports()
            .thenApply(this::mapReportsToResponseDtos)
            .thenApply(ResponseEntity::ok);
    }
    
    private List<ReportResponseDto> mapReportsToResponseDtos(List<Report> reports) {
        Map<String, String> studentNames = fetchStudentNames(reports);
        return reports.stream()
            .map(report -> {
                ReportResponseDto dto = ReportMapper.toDto(report);
                dto.setStudentName(studentNames.getOrDefault(report.getStudentId(), UNKNOWN_USER));
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    private Map<String, String> fetchStudentNames(List<Report> reports) {
        List<String> studentIds = reports.stream()
            .map(Report::getStudentId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<String, String> studentNames = new HashMap<>();
        if (!studentIds.isEmpty()) {
            for (String studentId : studentIds) {
                try {
                    fetchAndStoreStudentName(studentId, studentNames);
                } catch (Exception e) {
                    log.error("Error fetching student name for ID {}: {}", studentId, e.getMessage());
                    studentNames.put(studentId, UNKNOWN_USER);
                }
            }
        }
        return studentNames;
    }

    private void fetchAndStoreStudentName(String studentId, Map<String, String> studentNames) {
        Long id = parseStudentId(studentId);
        UserInfo userInfo = null;
        
        if (id != null) {
            studentNames.put(studentId, "User " + id); 
        } else {
            if (studentId.contains("@")) {
                userInfo = authServiceClient.getUserByEmail(studentId);
                if (userInfo != null) {
                    studentNames.put(studentId, userInfo.getName());
                } else {
                    studentNames.put(studentId, UNKNOWN_USER);
                }
            } else {
                studentNames.put(studentId, UNKNOWN_USER);
            }
        }
    }

    private Long parseStudentId(String studentId) {
        try {
            return Long.parseLong(studentId);
        } catch (NumberFormatException e) {
            return null;
        }
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
            response = ResponseEntity.notFound().build();
        } catch (udehnih.report.exception.InvalidReportStateException e) {
            response = ResponseEntity.badRequest().build();
        } catch (Exception e) {
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }
}
