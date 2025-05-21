package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;

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
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserReports() {
        Report report = ReportFactory.createOpenReport("12345", "Judul", "Detail");
        when(reportRepository.findByStudentId("12345"))
                .thenReturn(Arrays.asList(report));

        List<Report> reports = reportService.getUserReports("12345");

        assertEquals(1, reports.size());
        assertEquals("12345", reports.get(0).getStudentId());
    }

    @Test
    void testCreateReport() {
        Report report = ReportFactory.createOpenReport("12345", "Judul", "Detail");
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        Report result = reportService.createReport(report);

        assertNotNull(result);
        assertEquals(ReportStatus.OPEN, result.getStatus());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void testProcessReport() {
        Report existing = ReportFactory.createOpenReport("12345", "Judul", "Detail");
        when(reportRepository.findById(1)).thenReturn(Optional.of(existing));
        when(reportRepository.save(any())).thenReturn(existing);

        Report result = reportService.processReport(1);

        assertEquals(ReportStatus.RESOLVED, result.getStatus());
        verify(reportRepository).save(existing);
    }

    @Test
    void testProcessReportNotFound() {
        when(reportRepository.findById(99)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.processReport(99);
        });

        assertEquals("Report not found", exception.getMessage());
    }

    @Test
    void testUpdateReport() {
        Report existing = ReportFactory.createOpenReport("12345", "Old Title", "Old Detail");
        Report updated = ReportFactory.createOpenReport("12345", "New Title", "New Detail");

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
                ReportFactory.createOpenReport("12345", "Title1", "Detail1"),
                ReportFactory.createOpenReport("67890", "Title2", "Detail2")
        );

        when(reportRepository.findAll()).thenReturn(mockReports);

        List<Report> result = reportService.getAllReports();

        assertEquals(2, result.size());
        assertEquals("12345", result.get(0).getStudentId());
    }

    @Test
    void testUpdateReportNotFound() {
        when(reportRepository.findById(999)).thenReturn(Optional.empty());

        Report dummy = ReportFactory.createOpenReport("1", "dummy", "dummy");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.updateReport(999, dummy);
        });

        assertEquals("Report not found", exception.getMessage());
    }
}
