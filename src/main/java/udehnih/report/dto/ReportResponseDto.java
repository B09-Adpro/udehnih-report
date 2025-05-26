package udehnih.report.dto;
import lombok.Data;
import udehnih.report.enums.ReportStatus;
import udehnih.report.enums.RejectionMessage;
import java.time.LocalDateTime;
@Data
public class ReportResponseDto {
    private Integer reportId;
    private String studentId;
    private String studentName;
    private String title;
    private String detail;
    private ReportStatus status;
    private RejectionMessage rejectionMessage;
    private String rejectionMessageText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 