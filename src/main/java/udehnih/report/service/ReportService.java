package udehnih.report.service;
import udehnih.report.model.Report;
import udehnih.report.dto.RejectionRequestDto;
import java.util.List;
import java.util.concurrent.CompletableFuture;
public interface ReportService {
    Report createReport(Report report);
    CompletableFuture<List<Report>> getUserReports(String studentId);
    Report updateReport(Integer reportId, Report updatedReport); 
    void deleteReport(Integer reportId);
    CompletableFuture<List<Report>> getAllReports();
    Report processReport(Integer reportId, RejectionRequestDto rejectionRequest);
    CompletableFuture<Report> getReportById(Integer reportId);
}