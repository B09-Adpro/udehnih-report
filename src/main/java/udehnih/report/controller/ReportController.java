package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<Report> create(@RequestBody Report report) {
        Report created = reportService.createReport(report);
        return ResponseEntity.status(201).body(created); // HTTP 201 Created
    }

    @GetMapping
    public ResponseEntity<List<Report>> getByStudent(@RequestParam String studentId) {
        List<Report> reports = reportService.getReportsByStudentId(studentId);
        return ResponseEntity.ok(reports); // HTTP 200 OK
    }

    @PutMapping("/{id}")
    public ResponseEntity<Report> update(@PathVariable Integer id, @RequestBody Report report) {
        try {
            Report updated = reportService.updateReport(id, report);
            return ResponseEntity.ok(updated); // HTTP 200 OK
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // HTTP 404
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.noContent().build(); // HTTP 204
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // HTTP 404
        }
    }
}
