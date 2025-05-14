package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void testGetReportsByStudentId() throws Exception {
        Mockito.when(reportService.getReportsByStudentId("12345"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reports")
                .param("studentId", "12345"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateReport() throws Exception {
        Report dummy = new Report();
        dummy.setReportId(1);
        dummy.setStudentId("12345");
        dummy.setTitle("Test");
        dummy.setDetail("Test Detail");
        dummy.setStatus("OPEN");

        Mockito.when(reportService.createReport(any(Report.class)))
                .thenReturn(dummy);

        String json = """
                {
                    "studentId": "12345",
                    "title": "Test",
                    "detail": "Test Detail",
                    "status": "OPEN"
                }
                """;

        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdateReport() throws Exception {
        Report updated = new Report();
        updated.setReportId(1);
        updated.setStudentId("12345");
        updated.setTitle("Updated Title");
        updated.setDetail("Updated Detail");
        updated.setStatus("OPEN");

        Mockito.when(reportService.updateReport(eq(1), any(Report.class)))
                .thenReturn(updated);

        String json = """
                {
                    "studentId": "12345",
                    "title": "Updated Title",
                    "detail": "Updated Detail",
                    "status": "OPEN"
                }
                """;

        mockMvc.perform(put("/api/reports/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteReport() throws Exception {
        doNothing().when(reportService).deleteReport(1);

        mockMvc.perform(delete("/api/reports/1"))
                .andExpect(status().isNoContent());
    }
}
