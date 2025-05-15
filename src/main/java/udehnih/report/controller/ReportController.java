package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

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

    /** Default constructor. */
    public ReportController() {}

    /**
     * Create a new report.
     * @param request the report request DTO
     * @return the created report as a response DTO
     */
    @PostMapping
    public ResponseEntity<ReportResponseDto> create(final @RequestBody ReportRequestDto request) {
        Report created = reportService.createReport(ReportMapper.toEntity(request));
        return ResponseEntity.status(201).body(ReportMapper.toDto(created));
    }

    /**
     * Get reports by student ID.
     * @param studentId the student ID
     * @return list of report response DTOs
     */
    @GetMapping
    public ResponseEntity<List<ReportResponseDto>> getByStudent(final @RequestParam String studentId) {
        List<Report> reports = reportService.getReportsByStudentId(studentId);
        List<ReportResponseDto> dtos = reports.stream().map(ReportMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Update a report.
     * @param id the report ID
     * @param request the report request DTO
     * @return the updated report as a response DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReportResponseDto> update(final @PathVariable Integer id, final @RequestBody ReportRequestDto request) {
        try {
            Report updated = reportService.updateReport(id, ReportMapper.toEntity(request));
            return ResponseEntity.ok(ReportMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a report.
     * @param id the report ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(final @PathVariable Integer id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
