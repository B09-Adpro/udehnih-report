package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.exception.ReportNotFoundException;
import udehnih.report.exception.InvalidReportStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    @Async("reportTaskExecutor")
    public CompletableFuture<List<Report>> getUserReports(String studentId) {
        return reportRepository.findByStudentId(studentId);
    }

    @Override
    public Report updateReport(Integer reportId, Report updatedReport) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with id: " + reportId));

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
    @Async("reportTaskExecutor")
    public CompletableFuture<List<Report>> getAllReports() {
        return reportRepository.findAllAsync();
    }

    @Override
    @Transactional
    @Modifying
    public Report processReport(Integer reportId, RejectionRequestDto rejectionRequest) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with id: " + reportId));

        if (!report.isOpen()) {
            throw new InvalidReportStateException("Report cannot be processed because it is not in OPEN status");
        }

        LocalDateTime now = LocalDateTime.now();
        
        if (rejectionRequest != null && rejectionRequest.getRejectionMessage() != null) {
            report.setRejectionMessage(rejectionRequest.getRejectionMessage());
            report.setUpdatedAt(now);
            reportRepository.save(report);
            
            report.setStatus(ReportStatus.REJECTED);
            report.setUpdatedAt(now);
        } else {
            report.setRejectionMessage(null);
            report.setStatus(ReportStatus.RESOLVED);
            report.setUpdatedAt(now);
        }

        return reportRepository.save(report);
    }
}