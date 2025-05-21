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
        return ResponseEntity.ok(reports);
    }

    @PutMapping("/{reportId}")
    public ResponseEntity<Report> processReport(@PathVariable("reportId") Integer reportId) {
        try {
            Report processed = reportService.processReport(reportId);
            return ResponseEntity.ok(processed);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
