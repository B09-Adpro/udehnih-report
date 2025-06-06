package udehnih.report.service;
import udehnih.report.model.Report;
import udehnih.report.repository.ReportRepository;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.enums.RejectionMessage;
import udehnih.report.exception.ReportNotFoundException;
import udehnih.report.exception.InvalidReportStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

    void createReportShouldCreateOpenReport() {
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
        Report result = reportService.createReport(inputReport);
        assertNotNull(result);
        assertEquals(ReportStatus.OPEN, result.getStatus());
        verify(reportRepository).save(any(Report.class));
    }
    @Test

    void getUserReportsShouldReturnUserReports() throws ExecutionException, InterruptedException {
        String studentId = "12345";
        List<Report> expectedReports = Arrays.asList(
            ReportFactory.createOpenReport(studentId, "Report 1", "Detail 1"),
            ReportFactory.createOpenReport(studentId, "Report 2", "Detail 2")
        );
        when(reportRepository.findByStudentId(studentId))
            .thenReturn(CompletableFuture.completedFuture(expectedReports));
        CompletableFuture<List<Report>> futureResult = reportService.getUserReports(studentId);
        List<Report> result = futureResult.get(); 
        assertEquals(2, result.size());
        assertEquals(studentId, result.get(0).getStudentId());
        verify(reportRepository).findByStudentId(studentId);
    }
    @Test

    void updateReportWithValidReportShouldUpdateSuccessfully() {
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Old Title", "Old Detail");
        Report updatedReport = ReportFactory.createOpenReport("12345", "New Title", "New Detail");
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        Report result = reportService.updateReport(reportId, updatedReport);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Detail", result.getDetail());
        assertNotNull(result.getUpdatedAt());
        verify(reportRepository).save(any(Report.class));
    }
    @Test

    void updateReportWithNonExistentReportShouldThrowException() {
        Integer reportId = 999;
        Report updatedReport = ReportFactory.createOpenReport("12345", "New Title", "New Detail");
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(ReportNotFoundException.class, () -> {
            reportService.updateReport(reportId, updatedReport);
        });
        assertEquals("Report not found with id: " + reportId, exception.getMessage());
    }
    @Test

    void processReportWithValidRejectionShouldRejectReport() {
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Test", "Detail");
        RejectionRequestDto rejectionRequest = new RejectionRequestDto();
        rejectionRequest.setRejectionMessage(RejectionMessage.INCOMPLETE_DETAIL);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        Report result = reportService.processReport(reportId, rejectionRequest);
        assertEquals(ReportStatus.REJECTED, result.getStatus());
        assertEquals(RejectionMessage.INCOMPLETE_DETAIL, result.getRejectionMessage());
        verify(reportRepository, times(2)).save(any(Report.class));
    }
    @Test

    void processReportWithNoRejectionShouldResolveReport() {
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Test", "Detail");
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        Report result = reportService.processReport(reportId, null);
        assertEquals(ReportStatus.RESOLVED, result.getStatus());
        assertNull(result.getRejectionMessage());
        verify(reportRepository).save(any(Report.class));
    }
    @Test

    void processReportWithNonExistentReportShouldThrowException() {
        Integer reportId = 999;
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());
        Exception exception = assertThrows(ReportNotFoundException.class, () -> {
            reportService.processReport(reportId, null);
        });
        assertEquals("Report not found with id: " + reportId, exception.getMessage());
    }
    @Test

    void processReportWithNonOpenReportShouldThrowException() {
        Integer reportId = 1;
        Report existingReport = Report.builder()
                .reportId(reportId)
                .studentId("12345")
                .title("Test")
                .detail("Detail")
                .status(ReportStatus.RESOLVED)
                .build();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        Exception exception = assertThrows(InvalidReportStateException.class, () -> {
            reportService.processReport(reportId, null);
        });
        assertEquals("Report cannot be processed because it is not in OPEN status", exception.getMessage());
    }
    @Test

    void processReportWithEmptyRejectionMessageShouldResolveReport() {
        Integer reportId = 1;
        Report existingReport = ReportFactory.createOpenReport("12345", "Test", "Detail");
        RejectionRequestDto rejectionRequest = new RejectionRequestDto();
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(existingReport));
        when(reportRepository.save(any(Report.class))).thenAnswer(i -> i.getArgument(0));
        Report result = reportService.processReport(reportId, rejectionRequest);
        assertEquals(ReportStatus.RESOLVED, result.getStatus());
        assertNull(result.getRejectionMessage());
        verify(reportRepository).save(any(Report.class));
    }
    @Test

    void deleteReportShouldCallRepositoryDelete() {
        Integer reportId = 1;
        doNothing().when(reportRepository).deleteById(reportId);
        reportService.deleteReport(reportId);
        verify(reportRepository).deleteById(reportId);
    }
    @Test

    void getAllReportsShouldReturnAllReports() throws ExecutionException, InterruptedException {
        List<Report> expectedReports = Arrays.asList(
            ReportFactory.createOpenReport("12345", "Report 1", "Detail 1"),
            ReportFactory.createOpenReport("67890", "Report 2", "Detail 2")
        );
        when(reportRepository.findAllAsync())
            .thenReturn(CompletableFuture.completedFuture(expectedReports));
        CompletableFuture<List<Report>> futureResult = reportService.getAllReports();
        List<Report> result = futureResult.get(); 
        assertEquals(2, result.size());
        verify(reportRepository).findAllAsync();
    }
    
    @Test
    void getReportByIdShouldReturnReport() throws ExecutionException, InterruptedException {
        Integer reportId = 1;
        Report expectedReport = ReportFactory.createOpenReport("12345", "Test Report", "Test Detail");
        expectedReport.setReportId(reportId);
        
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(expectedReport));
        
        CompletableFuture<Report> futureResult = reportService.getReportById(reportId);
        Report result = futureResult.get();
        
        assertEquals(reportId, result.getReportId());
        assertEquals("Test Report", result.getTitle());
        assertEquals("Test Detail", result.getDetail());
        verify(reportRepository).findById(reportId);
    }
    
    @Test
    void getReportByIdShouldThrowExceptionWhenReportNotFound() {
        Integer reportId = 999;
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());
        
        CompletableFuture<Report> futureResult = reportService.getReportById(reportId);
        
        ExecutionException exception = assertThrows(ExecutionException.class, () -> {
            futureResult.get();
        });
        
        assertTrue(exception.getCause() instanceof ReportNotFoundException);
        assertEquals("Report not found with id: " + reportId, exception.getCause().getMessage());
        verify(reportRepository).findById(reportId);
    }
}
