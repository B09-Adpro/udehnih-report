package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Override
    public Report createReport(Report report) {
        Report newReport = ReportFactory.createOpenReport(report.getStudentId(), report.getTitle(), report.getDetail());
        return reportRepository.save(newReport);
    }

    @Override
    public List<Report> getUserReports(String studentId) {
        return reportRepository.findByStudentId(studentId);
    }

    @Override
    public Report updateReport(Integer reportId, Report updatedReport) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setTitle(updatedReport.getTitle());
        report.setDetail(updatedReport.getDetail());
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    @Override
    public void deleteReport(Integer reportId) {
        reportRepository.deleteById(reportId);
    }

    @Override
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @Override
    public Report processReport(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(ReportStatus.RESOLVED);
        report.setUpdatedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }
}