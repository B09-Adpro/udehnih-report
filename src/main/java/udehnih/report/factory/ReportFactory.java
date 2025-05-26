package udehnih.report.factory;
import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import java.time.LocalDateTime;
public final class ReportFactory {

    private ReportFactory() {
    }
    public

 static Report createOpenReport(final String studentId, final String title, final String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }
    public

 static Report createClosedReport(final String studentId, final String title, final String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .build();
    }
    public

 static Report createInProgressReport(final String studentId, final String title, final String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
    }
} 