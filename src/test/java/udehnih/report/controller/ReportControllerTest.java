package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.model.UserInfo;
import udehnih.report.service.ReportService;
import udehnih.report.client.AuthServiceClient;
import java.util.Arrays;
import udehnih.report.factory.ReportFactory;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.enums.ReportStatus;
import udehnih.report.config.TestConfig;
import udehnih.report.exception.ReportNotFoundException;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@WebMvcTest(ReportController.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
public class ReportControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private AuthServiceClient authServiceClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void getUserReportsWithValidStudentRoleReturnsReports() throws Exception {
        String studentId = "12345";
        String email = "student@example.com";
        
        UserInfo userInfo = UserInfo.builder()
            .id(Long.valueOf(studentId))
            .email(email)
            .name("Test Student")
            .roles(Arrays.asList("ROLE_STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(email)).thenReturn(userInfo);
        
        List<Report> reports = Arrays.asList(
            ReportFactory.createOpenReport(studentId, "Test Report 1", "Detail 1"),
            ReportFactory.createOpenReport(studentId, "Test Report 2", "Detail 2")
        );
        
        when(reportService.getUserReports(studentId))
            .thenReturn(CompletableFuture.completedFuture(reports));
        
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .param("studentId", studentId)
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].studentId").value(studentId))
                .andExpect(jsonPath("$[1].studentId").value(studentId));
    }
    @Test
    @WithMockUser(username = "staff@example.com", roles = {"STAFF"})
    void getUserReportsWithNonStudentRoleReturnsBadRequest() throws Exception {
        String studentId = "12345";
        String email = "staff@example.com";
        
        UserInfo userInfo = UserInfo.builder()
            .id(Long.valueOf(studentId))
            .email(email)
            .name("Test Staff")
            .roles(Arrays.asList("ROLE_STAFF"))
            .build();
        
        when(authServiceClient.getUserByEmail(email)).thenReturn(userInfo);
        
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .param("studentId", studentId)
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void getUserReportsWithMissingStudentIdReturnsBadRequest() throws Exception {
        String email = "student@example.com";
        
        when(authServiceClient.getUserByEmail(email)).thenReturn(null);
        
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound());
    }
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void getUserReportsWithEmptyStudentIdReturnsBadRequest() throws Exception {
        String email = "student@example.com";
        
        UserInfo userInfo = UserInfo.builder()
            .id(null)
            .email(email)
            .name("Test Student")
            .roles(Arrays.asList("ROLE_STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(email)).thenReturn(userInfo);
        
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .param("studentId", "")
                .with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();
        
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void createReportWithValidRequestReturnsCreated() throws Exception {
        String studentId = "12345";
        String email = "student@example.com";
        
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId(studentId);
        request.setTitle("Test Report");
        request.setDetail("Test Detail");
        
        UserInfo userInfo = UserInfo.builder()
            .id(Long.valueOf(studentId))
            .email(email)
            .name("Test Student")
            .roles(Arrays.asList("ROLE_STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(email)).thenReturn(userInfo);
        
        Report created = ReportFactory.createOpenReport(studentId, "Test Report", "Test Detail");
        when(reportService.createReport(any(Report.class))).thenReturn(created);
        
        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Report"))
                .andExpect(jsonPath("$.detail").value("Test Detail"))
                .andExpect(jsonPath("$.status").value(ReportStatus.OPEN.name()));
    }
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void updateReportWithValidRequestReturnsOk() throws Exception {
        Integer reportId = 1;
        String studentId = "12345";
        String email = "student@example.com";
        
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId(studentId);
        request.setTitle("Updated Report");
        request.setDetail("Updated Detail");
        
        UserInfo userInfo = UserInfo.builder()
            .id(Long.valueOf(studentId))
            .email(email)
            .name("Test Student")
            .roles(Arrays.asList("ROLE_STUDENT"))
            .build();
        
        when(authServiceClient.getUserByEmail(email)).thenReturn(userInfo);
        
        Report updated = ReportFactory.createOpenReport("12345", "Updated Report", "Updated Detail");
        when(reportService.updateReport(eq(reportId), any(Report.class))).thenReturn(updated);
        
        mockMvc.perform(put("/api/reports/{reportId}", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("12345"))
                .andExpect(jsonPath("$.title").value("Updated Report"))
                .andExpect(jsonPath("$.detail").value("Updated Detail"));
    }
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void updateReportWithNonExistentReportReturnsNotFound() throws Exception {
        Integer reportId = 999;
        
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId("12345");
        request.setTitle("Updated Report");
        request.setDetail("Updated Detail");
        
        when(reportService.updateReport(eq(reportId), any(Report.class)))
                .thenThrow(new ReportNotFoundException("Report not found with id: " + reportId));
        
        mockMvc.perform(put("/api/reports/{reportId}", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void deleteReportWithValidIdReturnsNoContent() throws Exception {
        Integer reportId = 1;
        
        doNothing().when(reportService).deleteReport(reportId);
        
        mockMvc.perform(delete("/api/reports/{reportId}", reportId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser(username = "student@example.com", roles = {"STUDENT"})
    void deleteReportWithNonExistentReportReturnsNotFound() throws Exception {
        Integer reportId = 999;
        
        doThrow(new ReportNotFoundException("Report not found with id: " + reportId))
                .when(reportService).deleteReport(reportId);
        
        mockMvc.perform(delete("/api/reports/{reportId}", reportId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
