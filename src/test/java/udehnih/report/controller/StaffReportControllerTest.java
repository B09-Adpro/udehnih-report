package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(StaffReportController.class)
public class StaffReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllReports() throws Exception {
        Mockito.when(reportService.getAllReports())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/staff/reports"))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessReport() throws Exception {
        Report dummy = ReportFactory.createInProgressReport("12345", "Test", "Test Detail");
        RejectionRequestDto rejectionRequest = null; // For approval case

        when(reportService.processReport(1, rejectionRequest)).thenReturn(dummy);

        mockMvc.perform(put("/api/staff/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.IN_PROGRESS.name()));
    }

    @Test
    void testProcessReportNotFound() throws Exception {
        Mockito.when(reportService.processReport(99, null))
               .thenThrow(new RuntimeException("Report not found"));

        mockMvc.perform(put("/api/staff/reports/99"))
                .andExpect(status().isNotFound());
    }
}
