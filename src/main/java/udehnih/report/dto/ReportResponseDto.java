package udehnih.report.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportResponseDto {
    private Integer reportId;
    private String studentId;
    private String title;
    private String detail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 