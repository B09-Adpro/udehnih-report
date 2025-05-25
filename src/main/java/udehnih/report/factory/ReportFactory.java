package udehnih.report.factory;

import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import java.time.LocalDateTime;

/**
 * Factory class for creating Report entities with different statuses.
 */
public final class ReportFactory {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ReportFactory() {
        // Utility class should not be instantiated
    }
    /**
     * Creates a new Report with OPEN status.
     * 
     * @param studentId the student ID
     * @param title the report title
     * @param detail the report details
     * @return a new Report entity with OPEN status
     */
    public static Report createOpenReport(final String studentId, final String title, final String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Report with CLOSED status.
     * 
     * @param studentId the student ID
     * @param title the report title
     * @param detail the report details
     * @return a new Report entity with CLOSED status
     */
    public static Report createClosedReport(final String studentId, final String title, final String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Report with IN_PROGRESS status.
     * 
     * @param studentId the student ID
     * @param title the report title
     * @param detail the report details
     * @return a new Report entity with IN_PROGRESS status
     */
    public static Report createInProgressReport(final String studentId, final String title, final String detail) {
        return Report.builder()
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(ReportStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
    }
} 