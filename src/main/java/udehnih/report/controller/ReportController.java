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
import java.util.stream.Collectors;

/**
 * REST controller for managing reports.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    /**
     * Service for report operations.
     */
    @Autowired
    private ReportService reportService;

    /**
     * Create a new report.
     * @param request the report request DTO
     * @return the created report as a response DTO
     */
    @PostMapping
    public ResponseEntity<ReportResponseDto> create(@RequestBody ReportRequestDto request) {
        Report created = reportService.createReport(ReportMapper.toEntity(request));
        return ResponseEntity.status(201).body(ReportMapper.toDto(created));
    }

    /**
     * Get reports by student ID.
     * @param studentId the student ID
     * @return list of report response DTOs
     */
    @GetMapping
    public ResponseEntity<List<ReportResponseDto>> getByStudent(@RequestParam String studentId) {
        List<Report> reports = reportService.getReportsByStudentId(studentId);
        List<ReportResponseDto> dtos = reports.stream().map(ReportMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Update a report.
     * @param reportId the report ID
     * @param request the report request DTO
     * @return the updated report as a response DTO
     */
    @PutMapping("/{reportId}")
    public ResponseEntity<ReportResponseDto> update(@PathVariable("reportId") Integer reportId, @RequestBody ReportRequestDto request) {
        try {
            Report updated = reportService.updateReport(reportId, ReportMapper.toEntity(request));
            return ResponseEntity.ok(ReportMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a report.
     * @param reportId the report ID
     * @return no content response
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> delete(@PathVariable("reportId") Integer reportId) {
        try {
            reportService.deleteReport(reportId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
