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

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponseDto> create(@RequestBody ReportRequestDto request) {
        Report created = reportService.createReport(ReportMapper.toEntity(request));
        return ResponseEntity.status(201).body(ReportMapper.toDto(created));
    }

    @GetMapping
    public ResponseEntity<List<ReportResponseDto>> getByStudent(@RequestParam String studentId) {
        List<Report> reports = reportService.getReportsByStudentId(studentId);
        List<ReportResponseDto> dtos = reports.stream().map(ReportMapper::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportResponseDto> update(@PathVariable Integer id, @RequestBody ReportRequestDto request) {
        try {
            Report updated = reportService.updateReport(id, ReportMapper.toEntity(request));
            return ResponseEntity.ok(ReportMapper.toDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
