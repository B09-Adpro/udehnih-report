package udehnih.report.dto;
import org.junit.jupiter.api.Test;
import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import udehnih.report.enums.RejectionMessage;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
class ReportMapperTest {
    @Test

    void toEntityWithValidDtoReturnsReport() {
        ReportRequestDto dto = new ReportRequestDto();
        dto.setStudentId("12345");
        dto.setTitle("Test Report");
        dto.setDetail("Test Detail");
        Report result = ReportMapper.toEntity(dto);
        assertNotNull(result);
        assertEquals("12345", result.getStudentId());
        assertEquals("Test Report", result.getTitle());
        assertEquals("Test Detail", result.getDetail());
    }
    @Test

    void toEntityWithNullDtoReturnsNull() {
        assertNull(ReportMapper.toEntity(null));
    }
    @Test

    void toDtoWithValidReportReturnsDto() {
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
        ReportResponseDto result = ReportMapper.toDto(report);
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

    void toDtoWithRejectedReportReturnsDtoWithRejectionMessage() {
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
        ReportResponseDto result = ReportMapper.toDto(report);
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

    void toDtoWithNullReportReturnsNull() {
        assertNull(ReportMapper.toDto(null));
    }
    @Test

    void toDtoWithNullRejectionMessageReturnsNullRejectionFields() {
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
        ReportResponseDto result = ReportMapper.toDto(report);
        assertNotNull(result);
        assertNull(result.getRejectionMessage());
        assertNull(result.getRejectionMessageText());
    }
    @Test

    void requestDtoSettersAndGetters() {
        ReportRequestDto dto = new ReportRequestDto();
        dto.setStudentId("s");
        dto.setTitle("t");
        dto.setDetail("d");
        assertEquals("s", dto.getStudentId());
        assertEquals("t", dto.getTitle());
        assertEquals("d", dto.getDetail());
    }
    @Test

    void responseDtoSettersAndGetters() {
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