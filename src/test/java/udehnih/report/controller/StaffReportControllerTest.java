package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;

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

    @Test
    void testGetAllReports() throws Exception {
        Mockito.when(reportService.getAllReports())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/staff/reports"))
                .andExpect(status().isOk());
    }

    @Test
    void testResolveReport() throws Exception {
        Report dummy = new Report();
        dummy.setReportId(1);
        dummy.setStatus("RESOLVED");

        when(reportService.resolveReport(1)).thenReturn(dummy);

        mockMvc.perform(put("/api/staff/reports/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
