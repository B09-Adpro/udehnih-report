package udehnih.report.service;

import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.enums.RejectionMessage;

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
    void createReport_ShouldCreateOpenReport() {
        // Arrange
        Report inputReport = Report.builder()
                .studentId("12345")
                .title("Test Report")
                .detail("Test Detail")
                .build();
        
        Report expectedReport = Report.builder()
                .studentId("12345")
                .title("Test Report")
                .detail("Test Detail")
                .status(ReportStatus.OPEN)
                .build();
        
        when(reportRepository.save(any(Report.class))).thenReturn(expectedReport);

        // Act
        Report result = reportService.createReport(inputReport);

        // Assert
        assertNotNull(result);
        assertEquals(ReportStatus.OPEN, result.getStatus());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void getUserReports_ShouldReturnUserReports() {
        // Arrange
        String studentId = "12345";
        List<Report> expectedReports = Arrays.asList(
            ReportFactory.createOpenReport(studentId, "Report 1", "Detail 1"),
            ReportFactory.createOpenReport(studentId, "Report 2", "Detail 2")
        );
        when(reportRepository.findByStudentId(studentId)).thenReturn(expectedReports);

        // Act
        List<Report> result = reportService.getUserReports(studentId);

        // Assert
        assertEquals(2, result.size());
        assertEquals(studentId, result.get(0).getStudentId());
        verify(reportRepository).findByStudentId(studentId);
    }

    @Test
    void updateReport_WithValidReport_ShouldUpdateSuccessfully() {
        // Arrange
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Old Title", "Old Detail");
        Report updatedReport = ReportFactory.createOpenReport("12345", "New Title", "New Detail");
        
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Report result = reportService.updateReport(reportId, updatedReport);

        // Assert
        assertEquals("New Title", result.getTitle());
        assertEquals("New Detail", result.getDetail());
        assertNotNull(result.getUpdatedAt());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void updateReport_WithNonExistentReport_ShouldThrowException() {
        // Arrange
        Integer reportId = 999;
        Report updatedReport = ReportFactory.createOpenReport("12345", "New Title", "New Detail");
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.updateReport(reportId, updatedReport);
        });
        assertEquals("Report not found", exception.getMessage());
    }

    @Test
    void processReport_WithValidRejection_ShouldRejectReport() {
        // Arrange
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Test", "Detail");
        RejectionRequestDto rejectionRequest = new RejectionRequestDto();
        rejectionRequest.setRejectionMessage(RejectionMessage.INCOMPLETE_DETAIL);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Report result = reportService.processReport(reportId, rejectionRequest);

        // Assert
        assertEquals(ReportStatus.REJECTED, result.getStatus());
        assertEquals(RejectionMessage.INCOMPLETE_DETAIL, result.getRejectionMessage());
        verify(reportRepository, times(2)).save(any(Report.class)); // Two saves: one for rejection message, one for status
    }

    @Test
    void processReport_WithNoRejection_ShouldResolveReport() {
        // Arrange
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Test", "Detail");
        
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Report result = reportService.processReport(reportId, null);

        // Assert
        assertEquals(ReportStatus.RESOLVED, result.getStatus());
        assertNull(result.getRejectionMessage());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void processReport_WithNonExistentReport_ShouldThrowException() {
        // Arrange
        Integer reportId = 999;
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.processReport(reportId, null);
        });
        assertEquals("Report not found", exception.getMessage());
    }

    @Test
    void processReport_WithNonOpenReport_ShouldThrowException() {
        // Arrange
        Integer reportId = 1;
        Report existingReport = Report.builder()
                .reportId(reportId)
                .studentId("12345")
                .title("Test")
                .detail("Detail")
                .status(ReportStatus.RESOLVED)
                .build();
        
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            reportService.processReport(reportId, null);
        });
        assertEquals("Report cannot be processed because it is not in OPEN status", exception.getMessage());
    }

    @Test
    void processReport_WithEmptyRejectionMessage_ShouldResolveReport() {
        // Arrange
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Test", "Detail");
        RejectionRequestDto rejectionRequest = new RejectionRequestDto(); // Empty rejection message
        
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Report result = reportService.processReport(reportId, rejectionRequest);

        // Assert
        assertEquals(ReportStatus.RESOLVED, result.getStatus());
        assertNull(result.getRejectionMessage());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void deleteReport_ShouldCallRepositoryDelete() {
        // Arrange
        Integer reportId = 1;
        doNothing().when(reportRepository).deleteById(reportId);

        // Act
        reportService.deleteReport(reportId);

        // Assert
        verify(reportRepository).deleteById(reportId);
    }

    @Test
    void getAllReports_ShouldReturnAllReports() {
        // Arrange
        List<Report> expectedReports = Arrays.asList(
            ReportFactory.createOpenReport("12345", "Report 1", "Detail 1"),
            ReportFactory.createOpenReport("67890", "Report 2", "Detail 2")
        );
        when(reportRepository.findAll()).thenReturn(expectedReports);

        // Act
        List<Report> result = reportService.getAllReports();

        // Assert
        assertEquals(2, result.size());
        verify(reportRepository).findAll();
    }
}
