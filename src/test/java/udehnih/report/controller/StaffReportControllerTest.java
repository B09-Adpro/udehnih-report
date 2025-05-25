package udehnih.report.controller;

import udehnih.report.model.Report;
import udehnih.report.service.ReportService;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import udehnih.report.dto.RejectionRequestDto;
import udehnih.report.exception.ReportNotFoundException;
import udehnih.report.config.TestConfig;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
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

    @Test
    void getAllReports_ReturnsEmptyList() throws Exception {
        when(reportService.getAllReports())
            .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        MvcResult result = mockMvc.perform(get("/api/staff/reports"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testProcessReport() throws Exception {
        Report dummy = ReportFactory.createInProgressReport("12345", "Test", "Test Detail");
        RejectionRequestDto rejectionRequest = null;

        when(reportService.processReport(1, rejectionRequest)).thenReturn(dummy);

        mockMvc.perform(put("/api/staff/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReportStatus.IN_PROGRESS.name()));
    }

    @Test
    void testProcessReportNotFound() throws Exception {
        Mockito.when(reportService.processReport(99, null))
               .thenThrow(new ReportNotFoundException("Report not found with id: 99"));

        mockMvc.perform(put("/api/staff/reports/99"))
                .andExpect(status().isNotFound());
    }
}
