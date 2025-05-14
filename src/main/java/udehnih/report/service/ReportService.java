package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    public Report createReport(Report report) {
        report.setStatus("OPEN");
        report.setCreatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public List<Report> getReportsByStudentId(String studentId) {
        return reportRepository.findByStudentId(studentId);
    }

    public Report updateReport(Integer id, Report updatedReport) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setTitle(updatedReport.getTitle());
        report.setDetail(updatedReport.getDetail());
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public void deleteReport(Integer id) {
        reportRepository.deleteById(id);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Report resolveReport(Integer id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus("RESOLVED");
        report.setUpdatedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }
}