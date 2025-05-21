package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.factory.ReportFactory;
import udehnih.report.dto.ReportRequestDto;
import udehnih.report.dto.ReportResponseDto;
import udehnih.report.dto.ReportMapper;
import udehnih.report.enums.ReportStatus;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void testGetUserReports() throws Exception {
        Report report = ReportFactory.createOpenReport("12345", "Test", "Test Detail");
        List<Report> reports = Collections.singletonList(report);
        Mockito.when(reportService.getUserReports("12345")).thenReturn(reports);

        mockMvc.perform(get("/api/reports")
                .param("studentId", "12345")
                .header("X-User-Email", "test@example.com")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("12345"));
    }

    @Test
    void testCreateReport() throws Exception {
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId("12345");
        request.setTitle("Test");
        request.setDetail("Test Detail");
        Report created = ReportFactory.createOpenReport("12345", "Test", "Test Detail");
        Mockito.when(reportService.createReport(any(Report.class))).thenReturn(created);

        String json = """
                {
                    "studentId": "12345",
                    "title": "Test",
                    "detail": "Test Detail"
                }
                """;

        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value("12345"))
                .andExpect(jsonPath("$.title").value("Test"))
                .andExpect(jsonPath("$.detail").value("Test Detail"))
                .andExpect(jsonPath("$.status").value(ReportStatus.OPEN.name()));
    }

    @Test
    void testUpdateReport() throws Exception {
        ReportRequestDto request = new ReportRequestDto();
        request.setStudentId("12345");
        request.setTitle("Updated Title");
        request.setDetail("Updated Detail");
        Report updated = ReportFactory.createOpenReport("12345", "Updated Title", "Updated Detail");
        Mockito.when(reportService.updateReport(eq(1), any(Report.class))).thenReturn(updated);

        String json = """
                {
                    "studentId": "12345",
                    "title": "Updated Title",
                    "detail": "Updated Detail"
                }
                """;

        mockMvc.perform(put("/api/reports/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("12345"))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.detail").value("Updated Detail"))
                .andExpect(jsonPath("$.status").value(ReportStatus.OPEN.name()));
    }

    @Test
    void testUpdateReportNotFound() throws Exception {
        Mockito.when(reportService.updateReport(eq(99), any(Report.class)))
               .thenThrow(new RuntimeException("Report not found"));

        String json = """
                {
                    "studentId": "12345",
                    "title": "Updated Title",
                    "detail": "Updated Detail"
                }
                """;

        mockMvc.perform(put("/api/reports/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteReport() throws Exception {
        doNothing().when(reportService).deleteReport(1);

        mockMvc.perform(delete("/api/reports/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteReportNotFound() throws Exception {
        Mockito.doThrow(new RuntimeException("Report not found"))
               .when(reportService).deleteReport(99);

        mockMvc.perform(delete("/api/reports/99"))
                .andExpect(status().isNotFound());
    }
}
