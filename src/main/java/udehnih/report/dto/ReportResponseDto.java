package udehnih.report.dto;

import lombok.Data;
import udehnih.report.enums.ReportStatus;
import java.time.LocalDateTime;

@Data
public class ReportResponseDto {
    private Integer reportId;
    private String studentId;
    private String title;
    private String detail;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 