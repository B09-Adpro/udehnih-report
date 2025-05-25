package udehnih.report.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.*;
import udehnih.report.enums.ReportStatus;
import udehnih.report.enums.RejectionMessage;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Report {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Integer reportId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Enumerated(STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) check (status in ('OPEN', 'CLOSED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'))")
    private ReportStatus status;

    @Enumerated(STRING)
    @Column(name = "rejection_message")
    private RejectionMessage rejectionMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isOpen() {
        return ReportStatus.OPEN.equals(this.status);
    }
}
