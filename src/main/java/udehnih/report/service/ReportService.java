package udehnih.report.service;

import udehnih.report.model.Report;
import java.util.List;

public interface ReportService {
    Report createReport(Report report);
    List<Report> getReportsByStudentId(String studentId);
    Report updateReport(Integer id, Report updatedReport);
    void deleteReport(Integer id);
    List<Report> getAllReports();
    Report resolveReport(Integer id);
}