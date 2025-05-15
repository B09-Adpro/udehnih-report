package udehnih.report.dto;

import org.junit.jupiter.api.Test;
import udehnih.report.model.Report;

import java.beans.Transient;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReportMapperTest {
    @Test
    void defaultMapperConstructor() {
        new ReportMapper();
    }

    @Test
    void toEntity_shouldMapFieldsCorrectly() {
        ReportRequestDto dto = new ReportRequestDto();
        dto.setStudentId("stu1");
        dto.setTitle("title");
        dto.setDetail("detail");

        Report report = ReportMapper.toEntity(dto);
        assertNotNull(report);
        assertEquals("stu1", report.getStudentId());
        assertEquals("title", report.getTitle());
        assertEquals("detail", report.getDetail());
    }

    @Test
    void toEntity_nullInputReturnsNull() {
        assertNull(ReportMapper.toEntity(null));
    }

    @Test
    void toDto_shouldMapFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reportId(10)
                .studentId("stu2")
                .title("t2")
                .detail("d2")
                .status("OPEN")
                .createdAt(now)
                .updatedAt(now)
                .build();
        ReportResponseDto dto = ReportMapper.toDto(report);
        assertNotNull(dto);
        assertEquals(10, dto.getReportId());
        assertEquals("stu2", dto.getStudentId());
        assertEquals("t2", dto.getTitle());
        assertEquals("d2", dto.getDetail());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }

    @Test
    void toDto_nullInputReturnsNull() {
        assertNull(ReportMapper.toDto(null));
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
        dto.setStatus("OPEN");
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);
        assertEquals(1, dto.getReportId());
        assertEquals("s", dto.getStudentId());
        assertEquals("t", dto.getTitle());
        assertEquals("d", dto.getDetail());
        assertEquals("OPEN", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }
} 