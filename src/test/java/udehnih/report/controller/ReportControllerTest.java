package udehnih.report.controller;
import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.service.CustomUserDetailsService;
import udehnih.report.factory.ReportFactory;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.enums.ReportStatus;
import udehnih.report.config.TestConfig;
import udehnih.report.exception.ReportNotFoundException;
import udehnih.report.util.JwtUtil;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private ObjectMapper objectMapper;
    @Test

    void getUserReportsWithValidStudentRoleReturnsReports() throws Exception {
        String studentId = "12345";
        String email = "student@example.com";
        String role = "STUDENT";
        String token = "valid-token";
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn(role);
        when(customUserDetailsService.getUserIdByEmail(email)).thenReturn(java.util.Optional.of(studentId));
        List<Report> reports = Arrays.asList(
            ReportFactory.createOpenReport(studentId, "Test Report 1", "Detail 1"),
            ReportFactory.createOpenReport(studentId, "Test Report 2", "Detail 2")
        );
        when(reportService.getUserReports(studentId))
            .thenReturn(CompletableFuture.completedFuture(reports));
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .param("studentId", studentId)
                .header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].studentId").value(studentId))
                .andExpect(jsonPath("$[1].studentId").value(studentId))
                .andExpect(jsonPath("$[0].title").value("Test Report 1"))
                .andExpect(jsonPath("$[1].title").value("Test Report 2"));
    }
    @Test

    void getUserReportsWithNonStudentRoleReturnsBadRequest() throws Exception {
        String studentId = "12345";
        String email = "staff@example.com";
        String role = "STAFF"; 
        String token = "valid-token";
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn(role);
        when(customUserDetailsService.getUserIdByEmail(email)).thenReturn(java.util.Optional.of(studentId));
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .param("studentId", studentId)
                .header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest());
    }
    @Test

    void getUserReportsWithMissingStudentIdReturnsBadRequest() throws Exception {
        String email = "student@example.com";
        String role = "STUDENT";
        String token = "valid-token";
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn(role);
        when(customUserDetailsService.getUserIdByEmail(email)).thenReturn(java.util.Optional.empty());
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest());
    }
    @Test

    void getUserReportsWithEmptyStudentIdReturnsBadRequest() throws Exception {
        String email = "student@example.com";
        String role = "STUDENT";
        String token = "valid-token";
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(jwtUtil.extractRole(token)).thenReturn(role);
        when(customUserDetailsService.getUserIdByEmail(email)).thenReturn(java.util.Optional.of(""));
        MvcResult mvcResult = mockMvc.perform(get("/api/reports")
                .param("studentId", "")
                .header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest());
    }
    @Test

    void createReportWithValidRequestReturnsCreated() throws Exception {
        String studentId = "12345";
        String email = "student@example.com";
        String token = "valid-token";
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId(studentId);
        request.setTitle("Test Report");
        request.setDetail("Test Detail");
        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(customUserDetailsService.getUserIdByEmail(email)).thenReturn(java.util.Optional.of(studentId));
        Report created = ReportFactory.createOpenReport(studentId, "Test Report", "Test Detail");
        when(reportService.createReport(any(Report.class))).thenReturn(created);
        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(studentId))
                .andExpect(jsonPath("$.title").value("Test Report"))
                .andExpect(jsonPath("$.detail").value("Test Detail"))
                .andExpect(jsonPath("$.status").value(ReportStatus.OPEN.name()));
    }
    @Test

    void updateReportWithValidRequestReturnsOk() throws Exception {
        Integer reportId = 1;
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId("12345");
        request.setTitle("Updated Report");
        request.setDetail("Updated Detail");
        Report updated = ReportFactory.createOpenReport("12345", "Updated Report", "Updated Detail");
        when(reportService.updateReport(eq(reportId), any(Report.class))).thenReturn(updated);
        mockMvc.perform(put("/api/reports/{reportId}", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("12345"))
                .andExpect(jsonPath("$.title").value("Updated Report"))
                .andExpect(jsonPath("$.detail").value("Updated Detail"));
    }
    @Test

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
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
    @Test

    void deleteReportWithValidIdReturnsNoContent() throws Exception {
        Integer reportId = 1;
        doNothing().when(reportService).deleteReport(reportId);
        mockMvc.perform(delete("/api/reports/{reportId}", reportId))
                .andExpect(status().isNoContent());
    }
    @Test

    void deleteReportWithNonExistentReportReturnsNotFound() throws Exception {
        Integer reportId = 999;
        doThrow(new ReportNotFoundException("Report not found with id: " + reportId))
                .when(reportService).deleteReport(reportId);
        mockMvc.perform(delete("/api/reports/{reportId}", reportId))
                .andExpect(status().isNotFound());
    }
}
