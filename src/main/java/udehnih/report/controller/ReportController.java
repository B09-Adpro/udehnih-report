package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(@RequestBody final ReportRequestDto request) {
        Report created = reportService.createReport(ReportMapper.toEntity(request));
        return ResponseEntity.status(201).body(ReportMapper.toDto(created));
    }

    @GetMapping
    public ResponseEntity<?> getUserReports(
        @RequestParam(required = false) String studentId,
        @RequestHeader("X-User-Email") String userEmail,
        @RequestHeader("X-User-Role") String userRole) {

        if (!userRole.equals("STUDENT")) {
            return ResponseEntity.badRequest().build();
        }
        
        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CompletableFuture<List<Report>> futureReports = reportService.getUserReports(studentId);
        CompletableFuture<ResponseEntity<List<ReportResponseDto>>> response = futureReports
            .thenApply(reports -> reports.stream()
                .map(ReportMapper::toDto)
                .collect(Collectors.toList()))
            .thenApply(ResponseEntity::ok);

        return ResponseEntity.ok().body(response);
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
