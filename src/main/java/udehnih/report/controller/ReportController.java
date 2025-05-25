package udehnih.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import udehnih.report.dto.ReportMapper;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.model.Report;
import udehnih.report.service.CustomUserDetailsService;
import udehnih.report.service.ReportService;
import udehnih.report.util.JwtUtil;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public ReportController(
        final ReportService reportService,
        final JwtUtil jwtUtil,
        final CustomUserDetailsService userDetailsService
    ) {
        this.reportService = reportService;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(
        @RequestBody final ReportRequestDto request,
        final HttpServletRequest httpRequest
    ) {
        // Get the JWT token directly from the request
        final String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        final String token = authHeader.substring(7);
        final String username = jwtUtil.extractUsername(token);

        log.info("Creating report for user: {}", username);

        // Look up the user ID based on the email
        final String studentId = userDetailsService
            .getUserIdByEmail(username)
            .orElse(null);

        if (studentId == null) {
            log.warn("Could not find studentId for email: {}", username);
            return ResponseEntity.status(404).build();
        }

        log.info("Found studentId: {} for email: {}", studentId, username);

        // Set the studentId in the request
        request.setStudentId(studentId);

        // Create a Report entity and explicitly set the studentId
        final Report report = ReportMapper.toEntity(request);

        // Double-check that studentId is set
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
        // Get the JWT token directly from the request
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CompletableFuture.completedFuture(ResponseEntity.status(401).body(List.of()));
        }
        
        final String token = authHeader.substring(7);
        final String username = jwtUtil.extractUsername(token);
        final String role = jwtUtil.extractRole(token);

        log.info("JWT token extracted directly in controller");
        log.info("Username from token: {}", username);
        log.info("Role from token: {}", role);

        // Check if the user has STUDENT role - only students can access reports
        if (!"STUDENT".equals(role)) {
            log.warn("User {} with role {} attempted to access student reports", username, role);
            return CompletableFuture.completedFuture(ResponseEntity.status(400).body(List.of()));
        }

        // Handle both case variants of the parameter
        String effectiveStudentId = studentId;
        if (isBlank(effectiveStudentId) && StudentId != null) {
            effectiveStudentId = StudentId;
        }

        // If no studentId provided, look it up based on the email from the JWT token
        if (isBlank(effectiveStudentId)) {
            log.info("No studentId provided in request, looking up based on email: {}", username);
            
            // Look up the user ID based on the email
            effectiveStudentId = userDetailsService.getUserIdByEmail(username).orElse(null);
            
            if (effectiveStudentId == null) {
                log.warn("Could not find studentId for email: {}", username);
                return CompletableFuture.completedFuture(ResponseEntity.status(400).body(List.of()));
            }
            
            log.info("Found studentId: {} for email: {}", effectiveStudentId, username);
        }

        // Check if the studentId is empty or blank
        if (isBlank(effectiveStudentId)) {
            log.warn("Empty or blank studentId provided");
            return CompletableFuture.completedFuture(ResponseEntity.status(400).body(List.of()));
        }

        log.info("Using studentId: {}", effectiveStudentId);

        // Capture the final studentId for use in lambda
        final String finalStudentId = effectiveStudentId;

        try {
            // Get the reports synchronously first to verify we can access them
            final List<Report> testReports = reportService.getUserReports(finalStudentId).join();
            log.info("Successfully retrieved {} reports for studentId: {}", testReports.size(), finalStudentId);
            
            // Now return the async version
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
        // Get the JWT token directly from the request
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(401).build()
            );
        }

        final String token = authHeader.substring(7);
        final String username = jwtUtil.extractUsername(token);

        log.info(
            "Retrieving report with ID: {} for user: {}",
            reportId,
            username
        );

        return reportService
            .getReportById(reportId)
            .thenApply(report -> {
                // Check if the user has permission to access this report
                final String userIdFromToken = userDetailsService
                    .getUserIdByEmail(username)
                    .orElse(null);
                final String role = jwtUtil.extractRole(token);

                // If user is not an admin and not the owner of the report, return 403
                if (
                    !"ADMIN".equals(role) &&
                    !report.getStudentId().equals(userIdFromToken)
                ) {
                    log.warn(
                        "User {} attempted to access report {} which belongs to {}",
                        username,
                        reportId,
                        report.getStudentId()
                    );
                    return ResponseEntity.status(403).<
                        ReportResponseDto
                    >build();
                }

                return ResponseEntity.ok(ReportMapper.toDto(report));
            })
            .exceptionally(ex -> {
                log.error("Error retrieving report: {}", ex.getMessage());
                return ResponseEntity.status(404).build();
            });
    }

    /**
     * Utility method to check if a string is null, empty, or contains only whitespace
     * @param str the string to check
     * @return true if the string is null, empty, or contains only whitespace
     */
    private static boolean isBlank(final String str) {
        return str == null || str.trim().isEmpty();
    }
}
