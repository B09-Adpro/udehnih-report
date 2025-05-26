package udehnih.report.dto;
import lombok.Data;
import udehnih.report.enums.RejectionMessage;
@Data
public class RejectionRequestDto {
    private RejectionMessage rejectionMessage;
} 