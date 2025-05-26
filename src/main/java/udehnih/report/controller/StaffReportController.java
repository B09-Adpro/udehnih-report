package udehnih.report.controller;
import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import udehnih.report.client.AuthServiceClient;
import udehnih.report.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
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
public class StaffReportController {
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
            .thenApply(reports -> {
                List<String> studentIds = reports.stream()
                    .map(Report::getStudentId)
                    .distinct()
                    .collect(Collectors.toList());
                Map<String, String> studentNames = new HashMap<>();
                if (!studentIds.isEmpty()) {
                    for (String studentId : studentIds) {
                        try {
                            Long id;
                            try {
                                id = Long.parseLong(studentId);
                            } catch (NumberFormatException e) {
                                id = null;
                            }
                            UserInfo userInfo = null;
                            if (id != null) {
                                studentNames.put(studentId, "User " + id); 
                            } else {
                                if (studentId.contains("@")) {
                                    userInfo = authServiceClient.getUserByEmail(studentId);
                                    if (userInfo != null) {
                                        studentNames.put(studentId, userInfo.getName());
                                    } else {
                                        studentNames.put(studentId, "Unknown");
                                    }
                                } else {
                                    studentNames.put(studentId, "Unknown");
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Error fetching student name for ID " + studentId + ": " + e.getMessage());
                            studentNames.put(studentId, "Unknown");
                        }
                    }
                }
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
            response = ResponseEntity.notFound().build();
        } catch (udehnih.report.exception.InvalidReportStateException e) {
            response = ResponseEntity.badRequest().build();
        } catch (Exception e) {
            response = ResponseEntity.internalServerError().build();
        }
        return response;
    }
}
