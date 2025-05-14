package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetReportsByStudentId() {
        Report report = new Report(1, "12345", "Judul", "Detail", "OPEN", null, null);
        when(reportRepository.findByStudentId("12345"))
                .thenReturn(Arrays.asList(report));

        List<Report> reports = reportService.getReportsByStudentId("12345");

        assertEquals(1, reports.size());
        assertEquals("12345", reports.get(0).getStudentId());
    }

    @Test
    void testCreateReport() {
        Report report = new Report(null, "12345", "Judul", "Detail", null, null, null);
        Report savedReport = new Report(1, "12345", "Judul", "Detail", "OPEN", LocalDateTime.now(), null);

        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        Report result = reportService.createReport(report);

        assertNotNull(result);
        assertEquals("OPEN", result.getStatus());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void testResolveReport() {
        Report existing = new Report(1, "12345", "Judul", "Detail", "OPEN", null, null);
        when(reportRepository.findById(1)).thenReturn(Optional.of(existing));
        when(reportRepository.save(any())).thenReturn(existing);

        Report result = reportService.resolveReport(1);

        assertEquals("RESOLVED", result.getStatus());
        verify(reportRepository).save(existing);
    }

    @Test
    void testResolveReportNotFound() {
        when(reportRepository.findById(99)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.resolveReport(99);
        });

        assertEquals("Report not found", exception.getMessage());
    }

    @Test
    void testUpdateReport() {
        Report existing = new Report(1, "12345", "Old Title", "Old Detail", "OPEN", null, null);
        Report updated = new Report(null, "12345", "New Title", "New Detail", "OPEN", null, null);

        when(reportRepository.findById(1)).thenReturn(Optional.of(existing));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        Report result = reportService.updateReport(1, updated);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Detail", result.getDetail());
        assertNotNull(result.getUpdatedAt());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void testDeleteReport() {
        Integer reportId = 1;
        doNothing().when(reportRepository).deleteById(reportId);

        reportService.deleteReport(reportId);

        verify(reportRepository).deleteById(reportId);
    }

    @Test
    void testGetAllReports() {
        List<Report> mockReports = Arrays.asList(
                new Report(1, "12345", "Title1", "Detail1", "OPEN", null, null),
                new Report(2, "67890", "Title2", "Detail2", "RESOLVED", null, null)
        );

        when(reportRepository.findAll()).thenReturn(mockReports);

        List<Report> result = reportService.getAllReports();

        assertEquals(2, result.size());
        assertEquals("12345", result.get(0).getStudentId());
    }

    @Test
    void testUpdateReportNotFound() {
        when(reportRepository.findById(999)).thenReturn(Optional.empty());

        Report dummy = new Report(null, "1", "dummy", "dummy", "OPEN", null, null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.updateReport(999, dummy);
        });

        assertEquals("Report not found", exception.getMessage());
    }
}
