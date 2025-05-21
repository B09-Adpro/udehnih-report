package udehnih.report.service;

import udehnih.report.model.Report;
import java.util.List;

public interface ReportService {
    Report createReport(Report report);
    List<Report> getUserReports(String studentId);
    Report updateReport(Integer reportId, Report updatedReport); 
    void deleteReport(Integer reportId);
    List<Report> getAllReports();
    Report processReport(Integer reportId);
}