package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

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
    @Transactional
    public Report processReport(Integer reportId, RejectionRequestDto rejectionRequest) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.isOpen()) {
            throw new RuntimeException("Report cannot be processed because it is not in OPEN status");
        }

        if (rejectionRequest != null && rejectionRequest.getRejectionMessage() != null) {
            report.setRejectionMessage(rejectionRequest.getRejectionMessage());
            report.setStatus(ReportStatus.REJECTED);
        } else {
            report.setStatus(ReportStatus.RESOLVED);
            report.setRejectionMessage(null);
        }
        
        report.setUpdatedAt(LocalDateTime.now());
        
        // The entity is already managed by JPA, no need to call save
        return report;
    }
}