package udehnih.report.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ReportTest {
    @Test
    void testReportBuilder() {
        Integer reportId = 1;
        String studentId = "12345";
        String title = "Test Report";
        String detail = "This is a test report";
        String status = "Pending";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        Report report = Report.builder()
                .reportId(reportId)
                .studentId(studentId)
                .title(title)
                .detail(detail)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertEquals(reportId, report.getReportId());
        assertEquals(studentId, report.getStudentId());
        assertEquals(title, report.getTitle());
        assertEquals(detail, report.getDetail());
        assertEquals(status, report.getStatus());
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
        assertNull(report.getCreatedAt());
        assertNull(report.getUpdatedAt());
    }

    @Test
    void testReportAllArgsConstructor() {
        Integer reportId = 1;
        String studentId = "12345";
        String title = "Test Report";
        String detail = "This is a test report";
        String status = "Pending";
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
        String status = "Pending";
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
        Report report1 = Report.builder()
            .reportId(1)
            .studentId("12345")
            .title("Test Report")
            .detail("This is a test report")
            .status("Pending")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        Report report2 = Report.builder()
                .reportId(2)
                .studentId("56789")
                .title("Test Report 2")
                .detail("This is a test report 2")
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
            .build();
        
        Report report3 = Report.builder()
               .reportId(1)
               .studentId("12345")
               .title("Test Report")
               .detail("This is a test report")
               .status("Pending")
               .createdAt(LocalDateTime.now())
               .updatedAt(LocalDateTime.now())
               .build();

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
        Report report = Report.builder()
               .reportId(1)
               .studentId("12345")
               .title("Test Report")
               .detail("This is a test report")
               .status("Pending")
               .createdAt(LocalDateTime.now())
               .updatedAt(LocalDateTime.now())
               .build();
        
        String toString = report.toString();

        assertTrue(toString.contains("1"));
        assertTrue(toString.contains("12345"));
        assertTrue(toString.contains("Test Report"));
        assertTrue(toString.contains("This is a test report"));
        assertTrue(toString.contains("Pending"));
    }
}
