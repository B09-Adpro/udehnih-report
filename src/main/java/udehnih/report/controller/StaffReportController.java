package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/reports")
public class StaffReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public CompletableFuture<ResponseEntity<List<ReportResponseDto>>> getAllReports() {
        return reportService.getAllReports()
            .thenApply(reports -> reports.stream()
                .map(ReportMapper::toDto)
                .collect(Collectors.toList()))
            .thenApply(ResponseEntity::ok);
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<ReportResponseDto> processReport(
            @PathVariable("reportId") Integer reportId,
            @RequestBody(required = false) RejectionRequestDto rejectionRequest) {
        try {
            Report processed = reportService.processReport(reportId, rejectionRequest);
            return ResponseEntity.ok(ReportMapper.toDto(processed));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
