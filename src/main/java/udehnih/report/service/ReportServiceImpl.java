package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
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
    public List<Report> getReportsByStudentId(String studentId) {
        return reportRepository.findByStudentId(studentId);
    }

    @Override
    public Report updateReport(Integer id, Report updatedReport) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setTitle(updatedReport.getTitle());
        report.setDetail(updatedReport.getDetail());
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    @Override
    public void deleteReport(Integer id) {
        reportRepository.deleteById(id);
    }

    @Override
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @Override
    public Report resolveReport(Integer id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus("CLOSED");
        report.setUpdatedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }
} 