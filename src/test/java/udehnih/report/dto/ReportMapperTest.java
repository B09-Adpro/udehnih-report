package udehnih.report.dto;

import org.junit.jupiter.api.Test;
import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import udehnih.report.enums.RejectionMessage;

import java.beans.Transient;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReportMapperTest {
    @Test
    void defaultMapperConstructor() {
        new ReportMapper();
    }

    @Test
    void toEntity_WithValidDto_ReturnsReport() {
        // Arrange
        ReportRequestDto dto = new ReportRequestDto();
        dto.setStudentId("12345");
        dto.setTitle("Test Report");
        dto.setDetail("Test Detail");

        // Act
        Report result = ReportMapper.toEntity(dto);

        // Assert
        assertNotNull(result);
        assertEquals("12345", result.getStudentId());
        assertEquals("Test Report", result.getTitle());
        assertEquals("Test Detail", result.getDetail());
    }

    @Test
    void toEntity_WithNullDto_ReturnsNull() {
        assertNull(ReportMapper.toEntity(null));
    }

    @Test
    void toDto_WithValidReport_ReturnsDto() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reportId(1)
                .studentId("12345")
                .title("Test Report")
                .detail("Test Detail")
                .status(ReportStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        ReportResponseDto result = ReportMapper.toDto(report);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getReportId());
        assertEquals("12345", result.getStudentId());
        assertEquals("Test Report", result.getTitle());
        assertEquals("Test Detail", result.getDetail());
        assertEquals(ReportStatus.OPEN, result.getStatus());
        assertNull(result.getRejectionMessage());
        assertNull(result.getRejectionMessageText());
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
    }

    @Test
    void toDto_WithRejectedReport_ReturnsDtoWithRejectionMessage() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reportId(1)
                .studentId("12345")
                .title("Test Report")
                .detail("Test Detail")
                .status(ReportStatus.REJECTED)
                .rejectionMessage(RejectionMessage.INCOMPLETE_DETAIL)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        ReportResponseDto result = ReportMapper.toDto(report);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getReportId());
        assertEquals("12345", result.getStudentId());
        assertEquals("Test Report", result.getTitle());
        assertEquals("Test Detail", result.getDetail());
        assertEquals(ReportStatus.REJECTED, result.getStatus());
        assertEquals(RejectionMessage.INCOMPLETE_DETAIL, result.getRejectionMessage());
        assertEquals("Detail laporan kurang lengkap", result.getRejectionMessageText());
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getUpdatedAt());
    }

    @Test
    void toDto_WithNullReport_ReturnsNull() {
        assertNull(ReportMapper.toDto(null));
    }

    @Test
    void toDto_WithNullRejectionMessage_ReturnsNullRejectionFields() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reportId(1)
                .studentId("12345")
                .title("Test Report")
                .detail("Test Detail")
                .status(ReportStatus.OPEN)
                .rejectionMessage(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Act
        ReportResponseDto result = ReportMapper.toDto(report);

        // Assert
        assertNotNull(result);
        assertNull(result.getRejectionMessage());
        assertNull(result.getRejectionMessageText());
    }

    @Test
    void requestDto_settersAndGetters() {
        ReportRequestDto dto = new ReportRequestDto();
        dto.setStudentId("s");
        dto.setTitle("t");
        dto.setDetail("d");
        assertEquals("s", dto.getStudentId());
        assertEquals("t", dto.getTitle());
        assertEquals("d", dto.getDetail());
    }

    @Test
    void responseDto_settersAndGetters() {
        ReportResponseDto dto = new ReportResponseDto();
        LocalDateTime now = LocalDateTime.now();
        dto.setReportId(1);
        dto.setStudentId("s");
        dto.setTitle("t");
        dto.setDetail("d");
        dto.setStatus(ReportStatus.OPEN);
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);
        assertEquals(1, dto.getReportId());
        assertEquals("s", dto.getStudentId());
        assertEquals("t", dto.getTitle());
        assertEquals("d", dto.getDetail());
        assertEquals(ReportStatus.OPEN, dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }
} 