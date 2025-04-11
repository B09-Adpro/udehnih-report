package udehnih.report.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;


@Table(name = "report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
    private UUID reportId;
    private String studentId;
    private String title;
    private String detail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
