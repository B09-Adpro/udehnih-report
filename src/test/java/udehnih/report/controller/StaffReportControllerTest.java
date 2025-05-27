package udehnih.report.controller;
import udehnih.report.model.Report;
import udehnih.report.model.UserInfo;
import udehnih.report.service.ReportService;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.exception.ReportNotFoundException;
import udehnih.report.exception.InvalidReportStateException;
import udehnih.report.config.TestConfig;
import udehnih.report.client.AuthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.contains;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
@WebMvcTest(StaffReportController.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
public class StaffReportControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private AuthServiceClient authServiceClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "staff@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
            )
        );
    }
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void getAllReportsReturnsEmptyList() throws Exception {
        when(reportService.getAllReports())
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        
        MvcResult result = mockMvc.perform(get("/api/staff/reports")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
    
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void getAllReportsReturnsReportsList() throws Exception {
        List<Report> reports = Arrays.asList(
            ReportFactory.createOpenReport("12345", "Test Report 1", "Detail 1"),
            ReportFactory.createOpenReport("67890", "Test Report 2", "Detail 2")
        );
        
        when(reportService.getAllReports())
            .thenReturn(CompletableFuture.completedFuture(reports));
        
        // Mock the AuthServiceClient for student name lookup
        UserInfo userInfo1 = UserInfo.builder()
            .id(12345L)
            .email("student1@example.com")
            .name("Student One")
            .roles(Arrays.asList("ROLE_STUDENT"))
            .build();
            
        when(authServiceClient.getUserByEmail(contains("12345"))).thenReturn(userInfo1);
        
        MvcResult result = mockMvc.perform(get("/api/staff/reports")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Test Report 1"))
                .andExpect(jsonPath("$[1].title").value("Test Report 2"));
    }
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void testProcessReport() throws Exception {
        Report dummy = ReportFactory.createInProgressReport("12345", "Test", "Test Detail");
        
        when(reportService.processReport(eq(1), any())).thenReturn(dummy);
        
        mockMvc.perform(put("/api/staff/reports/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.IN_PROGRESS.name()));
    }
    
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void testProcessReportWithRejection() throws Exception {
        Report dummy = ReportFactory.createOpenReport("12345", "Test", "Test Detail");
        dummy.setStatus(ReportStatus.REJECTED);
        
        RejectionRequestDto rejectionRequest = new RejectionRequestDto();
        rejectionRequest.setRejectionMessage(udehnih.report.enums.RejectionMessage.OTHER);
        
        when(reportService.processReport(1, rejectionRequest)).thenReturn(dummy);
        
        mockMvc.perform(put("/api/staff/reports/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectionRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.REJECTED.name()));
    }
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void testProcessReportNotFound() throws Exception {
        when(reportService.processReport(99, null))
               .thenThrow(new ReportNotFoundException("Report not found with id: 99"));
               
        mockMvc.perform(put("/api/staff/reports/99")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void testProcessReportInvalidState() throws Exception {
        when(reportService.processReport(eq(1), any()))
               .thenThrow(new InvalidReportStateException("Report is already processed"));
               
        mockMvc.perform(put("/api/staff/reports/1")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void testProcessReportServerError() throws Exception {
        when(reportService.processReport(eq(1), any()))
               .thenThrow(new RuntimeException("Unexpected server error"));
               
        mockMvc.perform(put("/api/staff/reports/1")
                .with(csrf()))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void getAllReportsWithServerError() throws Exception {
        when(reportService.getAllReports())
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        
        MvcResult result = mockMvc.perform(get("/api/staff/reports")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
    

    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void getAllReportsWithEmailLookupError() throws Exception {
        List<Report> reports = Arrays.asList(
            ReportFactory.createOpenReport("12345@test.com", "Test Report 1", "Detail 1")
        );
        
        when(reportService.getAllReports())
            .thenReturn(CompletableFuture.completedFuture(reports));
        
        when(authServiceClient.getUserByEmail(contains("@")))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        MvcResult result = mockMvc.perform(get("/api/staff/reports")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Unknown"));
    }
    
    @Test
    @WithMockUser(username = "staff@test.com", roles = {"STAFF"})
    void getAllReportsWithNullUserInfo() throws Exception {
        List<Report> reports = Arrays.asList(
            ReportFactory.createOpenReport("12345@test.com", "Test Report 1", "Detail 1")
        );
        
        when(reportService.getAllReports())
            .thenReturn(CompletableFuture.completedFuture(reports));
        
        when(authServiceClient.getUserByEmail(contains("@")))
            .thenReturn(null);
        
        MvcResult result = mockMvc.perform(get("/api/staff/reports")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Unknown"));
    }
}
