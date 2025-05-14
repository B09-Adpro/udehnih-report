package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/reports")
public class StaffReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports); // HTTP 200 OK
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Report> resolveReport(@PathVariable Integer id) {
        try {
            Report resolved = reportService.resolveReport(id);
            return ResponseEntity.ok(resolved); // HTTP 200 OK
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // HTTP 404
        }
    }
}
