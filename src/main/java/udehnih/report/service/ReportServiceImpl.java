package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.exception.ReportNotFoundException;
import udehnih.report.exception.InvalidReportStateException;
import udehnih.report.util.AppConstants;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    
    /**
     * Constructor for ReportServiceImpl.
     * 
     * @param reportRepository the repository for Report entities
     */
    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public Report createReport(final Report report) {
        final Report newReport = ReportFactory.createOpenReport(report.getStudentId(), report.getTitle(), report.getDetail());
        return reportRepository.save(newReport);
    }

    @Override
    @Async("reportTaskExecutor")
    public CompletableFuture<List<Report>> getUserReports(final String studentId) {
        return reportRepository.findByStudentId(studentId);
    }

    @Override
    public Report updateReport(final Integer reportId, final Report updatedReport) {
        final Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(AppConstants.REPORT_NOT_FOUND_MSG + reportId));

        report.setTitle(updatedReport.getTitle());
        report.setDetail(updatedReport.getDetail());
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    @Override
    public void deleteReport(final Integer reportId) {
        reportRepository.deleteById(reportId);
    }

    @Override
    @Async("reportTaskExecutor")
    public CompletableFuture<List<Report>> getAllReports() {
        return reportRepository.findAllAsync();
    }
    
    @Override
    @Async("reportTaskExecutor")
    public CompletableFuture<Report> getReportById(final Integer reportId) {
        return CompletableFuture.supplyAsync(() -> 
            reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with id: " + reportId))
        );
    }

    @Override
    @Transactional
    @Modifying
    public Report processReport(final Integer reportId, final RejectionRequestDto rejectionRequest) {
        final Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(AppConstants.REPORT_NOT_FOUND_MSG + reportId));

        if (!report.isOpen()) {
            throw new InvalidReportStateException("Report cannot be processed because it is not in OPEN status");
        }

        final LocalDateTime now = LocalDateTime.now();
        
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