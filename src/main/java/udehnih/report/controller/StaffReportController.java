package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import org.springframework.http.ResponseEntity;
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
public class StaffReportController {

    private final ReportService reportService;
    
    public StaffReportController(ReportService reportService) {
        this.reportService = reportService;
    }

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
