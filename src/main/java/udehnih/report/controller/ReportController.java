package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.service.CustomUserDetailsService;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import udehnih.report.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(
        @RequestBody final ReportRequestDto request,
        HttpServletRequest httpRequest) {
        
        // Get the JWT token directly from the request
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        
        log.info("Creating report for user: {}", username);
        
        // Look up the user ID based on the email
        String studentId = userDetailsService.getUserIdByEmail(username)
            .orElse(null);
        
        if (studentId == null) {
            log.warn("Could not find studentId for email: {}", username);
            return ResponseEntity.status(404).build();
        }
        
        log.info("Found studentId: {} for email: {}", studentId, username);
        
        // Set the studentId in the request
        request.setStudentId(studentId);
        
        // Create a Report entity and explicitly set the studentId
        Report report = ReportMapper.toEntity(request);
        
        // Double-check that studentId is set
        if (report.getStudentId() == null) {
            log.warn("StudentId is null after mapping, setting it explicitly");
            report.setStudentId(studentId);
        }
        
        log.info("Creating report with studentId: {}", report.getStudentId());
        
        Report created = reportService.createReport(report);
        return ResponseEntity.status(201).body(ReportMapper.toDto(created));
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<ReportResponseDto>>> getUserReports(
        @RequestParam(required = false) String studentId,
        @RequestParam(required = false) String StudentId,
        HttpServletRequest request) {
        
        // Get the JWT token directly from the request
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CompletableFuture.completedFuture(ResponseEntity.status(401).body(List.of()));
        }
        
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        
        log.info("JWT token extracted directly in controller");
        log.info("Username from token: {}", username);
        log.info("Role from token: {}", role);
        
        // Handle both case variants of the parameter
        String effectiveStudentId = studentId;
        if ((effectiveStudentId == null || effectiveStudentId.trim().isEmpty()) && StudentId != null) {
            effectiveStudentId = StudentId;
        }
        
        // If no studentId provided, look it up based on the email from the JWT token
        if (effectiveStudentId == null || effectiveStudentId.trim().isEmpty()) {
            log.info("No studentId provided in request, looking up based on email: {}", username);
            
            // Look up the user ID based on the email
            effectiveStudentId = userDetailsService.getUserIdByEmail(username)
                .orElse(null);
            
            if (effectiveStudentId == null) {
                log.warn("Could not find studentId for email: {}", username);
                return CompletableFuture.completedFuture(ResponseEntity.status(404)
                    .body(List.of()));
            }
            
            log.info("Found studentId: {} for email: {}", effectiveStudentId, username);
        }
        
        log.info("Using studentId: {}", effectiveStudentId);
        
        // Capture the final studentId for use in lambda
        final String finalStudentId = effectiveStudentId;
        
        try {
            // Get the reports synchronously first to verify we can access them
            List<Report> testReports = reportService.getUserReports(finalStudentId).join();
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
    public ResponseEntity<ReportResponseDto> updateReport(@PathVariable("reportId") Integer reportId, @RequestBody ReportRequestDto request) {
        try {
            Report updated = reportService.updateReport(reportId, ReportMapper.toEntity(request));
            return ResponseEntity.ok(ReportMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{reportId}") 
    public ResponseEntity<Void> deleteReport(@PathVariable("reportId") Integer reportId) {
        try {
            reportService.deleteReport(reportId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
