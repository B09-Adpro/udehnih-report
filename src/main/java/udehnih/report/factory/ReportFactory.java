package udehnih.report.factory;

import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import java.time.LocalDateTime;

public class ReportFactory {
    public static Report createOpenReport(String studentId, String title, String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Report createClosedReport(String studentId, String title, String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Report createInProgressReport(String studentId, String title, String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
    }
} 