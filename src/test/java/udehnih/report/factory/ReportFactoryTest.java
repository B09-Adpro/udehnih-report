package udehnih.report.factory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import java.time.LocalDateTime;
class ReportFactoryTest {
    private static final String STUDENT_ID = "123";
    private static final String TITLE = "Test Report";
    private static final String DETAIL = "Test Detail";
    @Test

    void createOpenReportShouldCreateReportWithOpenStatus() {
        Report report = ReportFactory.createOpenReport(STUDENT_ID, TITLE, DETAIL);
        assertNotNull(report);
        assertEquals(STUDENT_ID, report.getStudentId());
        assertEquals(TITLE, report.getTitle());
        assertEquals(DETAIL, report.getDetail());
        assertEquals(ReportStatus.OPEN, report.getStatus());
        assertNotNull(report.getCreatedAt());
        assertTrue(report.isOpen());
    }
    @Test

    void createClosedReportShouldCreateReportWithClosedStatus() {
        Report report = ReportFactory.createClosedReport(STUDENT_ID, TITLE, DETAIL);
        assertNotNull(report);
        assertEquals(STUDENT_ID, report.getStudentId());
        assertEquals(TITLE, report.getTitle());
        assertEquals(DETAIL, report.getDetail());
        assertEquals(ReportStatus.CLOSED, report.getStatus());
        assertNotNull(report.getCreatedAt());
        assertFalse(report.isOpen());
    }
    @Test

    void createInProgressReportShouldCreateReportWithInProgressStatus() {
        Report report = ReportFactory.createInProgressReport(STUDENT_ID, TITLE, DETAIL);
        assertNotNull(report);
        assertEquals(STUDENT_ID, report.getStudentId());
        assertEquals(TITLE, report.getTitle());
        assertEquals(DETAIL, report.getDetail());
        assertEquals(ReportStatus.IN_PROGRESS, report.getStatus());
        assertNotNull(report.getCreatedAt());
        assertFalse(report.isOpen());
    }
    @Test

    void createOpenReportNullValues() {
        Report report = ReportFactory.createOpenReport(null, null, null);
        assertNotNull(report);
        assertNull(report.getStudentId());
        assertNull(report.getTitle());
        assertNull(report.getDetail());
        assertEquals(ReportStatus.OPEN, report.getStatus());
        assertNotNull(report.getCreatedAt());
    }
    @Test

    void createClosedReportEmptyStrings() {
        Report report = ReportFactory.createClosedReport("", "", "");
        assertNotNull(report);
        assertEquals("", report.getStudentId());
        assertEquals("", report.getTitle());
        assertEquals("", report.getDetail());
        assertEquals(ReportStatus.CLOSED, report.getStatus());
        assertNotNull(report.getCreatedAt());
    }
    @Test

    void createInProgressReportNullAndEmpty() {
        Report report = ReportFactory.createInProgressReport(null, "", null);
        assertNotNull(report);
        assertNull(report.getStudentId());
        assertEquals("", report.getTitle());
        assertNull(report.getDetail());
        assertEquals(ReportStatus.IN_PROGRESS, report.getStatus());
        assertNotNull(report.getCreatedAt());
    }
    @Test

    void createdAtIsRecent() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Report report = ReportFactory.createOpenReport(STUDENT_ID, TITLE, DETAIL);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(report.getCreatedAt().isAfter(before));
        assertTrue(report.getCreatedAt().isBefore(after));
    }
} 