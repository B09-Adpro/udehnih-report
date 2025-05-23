package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.service.AuthService;
import udehnih.report.factory.ReportFactory;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import udehnih.report.enums.ReportStatus;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getUserReports_WithValidStudentRole_ReturnsReports() throws Exception {
        String studentId = "12345";
        List<Report> reports = Arrays.asList(
            ReportFactory.createOpenReport(studentId, "Test Report 1", "Detail 1"),
            ReportFactory.createOpenReport(studentId, "Test Report 2", "Detail 2")
        );
        when(reportService.getUserReports(studentId)).thenReturn(reports);

        mockMvc.perform(get("/api/reports")
                .param("studentId", studentId)
                .header("X-User-Email", "student@example.com")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId))
                .andExpect(jsonPath("$[1].studentId").value(studentId))
                .andExpect(jsonPath("$[0].title").value("Test Report 1"))
                .andExpect(jsonPath("$[1].title").value("Test Report 2"));
    }

    @Test
    void getUserReports_WithNonStudentRole_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports")
                .param("studentId", "12345")
                .header("X-User-Email", "staff@example.com")
                .header("X-User-Role", "STAFF"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserReports_WithMissingStudentId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports")
                .header("X-User-Email", "student@example.com")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserReports_WithEmptyStudentId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports")
                .param("studentId", "")
                .header("X-User-Email", "student@example.com")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReport_WithValidRequest_ReturnsCreated() throws Exception {
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId("12345");
        request.setTitle("Test Report");
        request.setDetail("Test Detail");

        Report created = ReportFactory.createOpenReport("12345", "Test Report", "Test Detail");
        when(reportService.createReport(any(Report.class))).thenReturn(created);

        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value("12345"))
                .andExpect(jsonPath("$.title").value("Test Report"))
                .andExpect(jsonPath("$.detail").value("Test Detail"))
                .andExpect(jsonPath("$.status").value(ReportStatus.OPEN.name()));
    }

    @Test
    void updateReport_WithValidRequest_ReturnsOk() throws Exception {
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
    void updateReport_WithNonExistentReport_ReturnsNotFound() throws Exception {
        Integer reportId = 999;
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId("12345");
        request.setTitle("Updated Report");
        request.setDetail("Updated Detail");

        when(reportService.updateReport(eq(reportId), any(Report.class)))
                .thenThrow(new RuntimeException("Report not found"));

        mockMvc.perform(put("/api/reports/{reportId}", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReport_WithValidId_ReturnsNoContent() throws Exception {
        Integer reportId = 1;
        doNothing().when(reportService).deleteReport(reportId);

        mockMvc.perform(delete("/api/reports/{reportId}", reportId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReport_WithNonExistentReport_ReturnsNotFound() throws Exception {
        Integer reportId = 999;
        doThrow(new RuntimeException("Report not found"))
                .when(reportService).deleteReport(reportId);

        mockMvc.perform(delete("/api/reports/{reportId}", reportId))
                .andExpect(status().isNotFound());
    }
}
