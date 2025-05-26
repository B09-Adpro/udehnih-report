package udehnih.report.controller;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import udehnih.report.client.AuthServiceClient;
import udehnih.report.dto.ReportMapper;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.model.Report;
import udehnih.report.model.UserInfo;
import udehnih.report.service.ReportService;
@Slf4j

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    @Autowired
    private AuthServiceClient authServiceClient;

    public ReportController(final ReportService reportService) {
        this.reportService = reportService;
    }
    @PostMapping

    public ResponseEntity<ReportResponseDto> createReport(
        @RequestBody final ReportRequestDto request,
        final HttpServletRequest httpRequest
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        log.info("Creating report for user: {}", username);
        UserInfo userInfo = authServiceClient.getUserByEmail(username);
        if (userInfo == null || userInfo.getId() == null) {
            log.warn("Could not find user info for email: {}", username);
            return ResponseEntity.status(404).build();
        }
        final String studentId = userInfo.getId().toString();
        log.info("Found studentId: {} for email: {}", studentId, username);
        request.setStudentId(studentId);
        final Report report = ReportMapper.toEntity(request);
        if (report.getStudentId() == null) {
            log.warn("StudentId is null after mapping, setting it explicitly");
            report.setStudentId(studentId);
        }
        log.info("Creating report with studentId: {}", report.getStudentId());
        final Report created = reportService.createReport(report);
        return ResponseEntity.status(201).body(ReportMapper.toDto(created));
    }
    @GetMapping

    public CompletableFuture<ResponseEntity<List<ReportResponseDto>>> getUserReports(
        @RequestParam(required = false) final String studentId,
        @RequestParam(required = false) final String StudentId,
        final HttpServletRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(401).body(List.of()));
        }
        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        log.info("Getting reports for authenticated user: {}", username);
        UserInfo userInfo = authServiceClient.getUserByEmail(username);
        if (userInfo == null) {
            log.warn("Could not find user info for email: {}", username);
            return CompletableFuture.completedFuture(ResponseEntity.status(404).body(List.of()));
        }
        if (!userInfo.isStudent()) {
            log.warn("User {} with roles {} attempted to access student reports but lacks STUDENT role", 
                    username, userInfo.getRoles());
            return CompletableFuture.completedFuture(ResponseEntity.status(403).body(List.of()));
        }
        log.info("User {} has STUDENT role, allowing access to reports", username);
        String effectiveStudentId = studentId;
        if (isBlank(effectiveStudentId) && StudentId != null) {
            effectiveStudentId = StudentId;
        }
        if (isBlank(effectiveStudentId)) {
            log.info("No studentId provided in request, using authenticated user ID");
            effectiveStudentId = userInfo.getId().toString();
            log.info("Using authenticated user ID: {} for email: {}", effectiveStudentId, username);
        }
        if (isBlank(effectiveStudentId)) {
            log.warn("Empty or blank studentId provided");
            return CompletableFuture.completedFuture(ResponseEntity.status(400).body(List.of()));
        }
        log.info("Using studentId: {}", effectiveStudentId);
        final String finalStudentId = effectiveStudentId;
        try {
            final List<Report> testReports = reportService.getUserReports(finalStudentId).join();
            log.info("Successfully retrieved {} reports for studentId: {}", testReports.size(), finalStudentId);
            return reportService.getUserReports(finalStudentId)
                .thenApply(reports -> reports.stream()
                    .map(ReportMapper::toDto)
                    .collect(Collectors.toList()))
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error retrieving reports: {}", ex.getMessage());
                    return ResponseEntity.status(500).body(List.of());
                });
        } catch (Exception e) {
            log.error("Error testing report access: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.status(500).body(List.of()));
        }
    }
    @PutMapping("/{reportId}")

    public ResponseEntity<ReportResponseDto> updateReport(
        @PathVariable("reportId") final Integer reportId,
        @RequestBody final ReportRequestDto request
    ) {
        ResponseEntity<ReportResponseDto> response;
        try {
            final Report updated = reportService.updateReport(
                reportId,
                ReportMapper.toEntity(request)
            );
            response = ResponseEntity.ok(ReportMapper.toDto(updated));
        } catch (RuntimeException e) {
            log.warn("Report not found or invalid data: {}", e.getMessage());
            response = ResponseEntity.notFound().build();
        }
        return response;
    }
    @DeleteMapping("/{reportId}")

    public ResponseEntity<Void> deleteReport(
        @PathVariable("reportId") final Integer reportId
    ) {
        ResponseEntity<Void> response;
        try {
            reportService.deleteReport(reportId);
            response = ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("Failed to delete report: {}", e.getMessage());
            response = ResponseEntity.notFound().build();
        }
        return response;
    }
    @GetMapping("/{reportId}")

    public CompletableFuture<ResponseEntity<ReportResponseDto>> getReportById(
        @PathVariable("reportId") final Integer reportId,
        final HttpServletRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(401).build());
        }
        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getName();
        }
        log.info("Retrieving report with ID: {} for user: {}", reportId, username);
        UserInfo userInfo = authServiceClient.getUserByEmail(username);
        if (userInfo == null) {
            log.warn("Could not find user info for email: {}", username);
            return CompletableFuture.completedFuture(ResponseEntity.status(404).build());
        }
        return reportService
            .getReportById(reportId)
            .thenApply(report -> {
                String userId = userInfo.getId().toString();
                if (!userInfo.isStaff() && !report.getStudentId().equals(userId)) {
                    log.warn(
                        "User {} attempted to access report {} which belongs to {}",
                        username,
                        reportId,
                        report.getStudentId()
                    );
                    return ResponseEntity.status(403).<ReportResponseDto>build();
                }
                return ResponseEntity.ok(ReportMapper.toDto(report));
            })
            .exceptionally(ex -> {
                log.error("Error retrieving report: {}", ex.getMessage());
                return ResponseEntity.status(404).build();
            });
    }
    private

 static boolean isBlank(final String str) {
        return str == null || str.trim().isEmpty();
    }
}
