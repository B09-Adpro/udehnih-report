package udehnih.report.model;

import org.junit.jupiter.api.Test;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.enums.RejectionMessage;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ReportTest {
    @Test
    void testReportBuilder() {
        Integer reportId = 1;
        String studentId = "12345";
        String title = "Test Report";
        String detail = "This is a test report";
        ReportStatus status = ReportStatus.OPEN;
        RejectionMessage rejectionMessage = RejectionMessage.INCOMPLETE_DETAIL;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        Report report = Report.builder()
                .reportId(reportId)
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(status)
                .rejectionMessage(rejectionMessage)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertEquals(reportId, report.getReportId());
        assertEquals(studentId, report.getStudentId());
        assertEquals(title, report.getTitle());
        assertEquals(detail, report.getDetail());
        assertEquals(status, report.getStatus());
        assertEquals(rejectionMessage, report.getRejectionMessage());
        assertEquals(createdAt, report.getCreatedAt());
        assertEquals(updatedAt, report.getUpdatedAt());
    }

    @Test
    void testReportNoArgsConstructor() {
        Report report = new Report();
        assertNull(report.getReportId());
        assertNull(report.getStudentId());
        assertNull(report.getTitle());
        assertNull(report.getDetail());
        assertNull(report.getStatus());
        assertNull(report.getRejectionMessage());
        assertNull(report.getCreatedAt());
        assertNull(report.getUpdatedAt());
    }

    @Test
    void testReportAllArgsConstructor() {
        Integer reportId = 1;
        String studentId = "12345";
        String title = "Test Report";
        String detail = "This is a test report";
        ReportStatus status = ReportStatus.OPEN;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        Report report = new Report(reportId, studentId, title, detail, status, createdAt, updatedAt);

        assertEquals(reportId, report.getReportId());
        assertEquals(studentId, report.getStudentId());
        assertEquals(title, report.getTitle());
        assertEquals(detail, report.getDetail());
        assertEquals(status, report.getStatus());
        assertEquals(createdAt, report.getCreatedAt());
        assertEquals(updatedAt, report.getUpdatedAt());
    }

    @Test
    void testReportSettersAndGetters() {
        Report report = new Report();
        Integer reportId = 1;
        String studentId = "12345";
        String title = "Test Report";
        String detail = "This is a test report";
        ReportStatus status = ReportStatus.OPEN;
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        report.setReportId(reportId);
        report.setStudentId(studentId);
        report.setTitle(title);
        report.setDetail(detail);
        report.setStatus(status);
        report.setCreatedAt(createdAt);
        report.setUpdatedAt(updatedAt);

        assertEquals(reportId, report.getReportId());
        assertEquals(studentId, report.getStudentId());
        assertEquals(title, report.getTitle());
        assertEquals(detail, report.getDetail());
        assertEquals(status, report.getStatus());
        assertEquals(createdAt, report.getCreatedAt());
        assertEquals(updatedAt, report.getUpdatedAt());
    }

    @Test
    void testReportEqualsAndHashCode() {
        Report report1 = ReportFactory.createOpenReport("12345", "Test Report", "This is a test report");
        Report report2 = ReportFactory.createClosedReport("56789", "Test Report 2", "This is a test report 2");
        Report report3 = ReportFactory.createOpenReport("12345", "Test Report", "This is a test report");

        report1.setReportId(1);
        report2.setReportId(2);
        report3.setReportId(1);

        assertEquals(report1, report1);
        assertEquals(report1, report3);
        assertNotEquals(report1, report2);
        assertNotEquals(report1, null);
        assertNotEquals(report1, new Object());

        assertEquals(report1.hashCode(), report1.hashCode());
        assertNotEquals(report1.hashCode(), report2.hashCode());
    }

    @Test
    void testReportToString() {
        Report report = ReportFactory.createOpenReport("12345", "Test Report", "This is a test report");
        String toString = report.toString();
        assertTrue(toString.contains("12345"));
        assertTrue(toString.contains("Test Report"));
        assertTrue(toString.contains("This is a test report"));
        assertTrue(toString.contains(ReportStatus.OPEN.name()));
    }

    @Test
    void testIsOpenWithFactory() {
        Report openReport = ReportFactory.createOpenReport("1", "t", "d");
        Report closedReport = ReportFactory.createClosedReport("1", "t", "d");
        Report inProgressReport = ReportFactory.createInProgressReport("1", "t", "d");
        assertTrue(openReport.isOpen());
        assertFalse(closedReport.isOpen());
        assertFalse(inProgressReport.isOpen());
    }
}
