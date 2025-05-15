package udehnih.report.factory;

import udehnih.report.model.Report;
import java.time.LocalDateTime;

public class ReportFactory {
    public static Report createOpenReport(String studentId, String title, String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Report createClosedReport(String studentId, String title, String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status("CLOSED")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Report createInProgressReport(String studentId, String title, String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status("IN_PROGRESS")
                .createdAt(LocalDateTime.now())
                .build();
    }
} 