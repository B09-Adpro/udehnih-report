package udehnih.report.repository;

import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    List<Report> findByStudentId(String studentId);

    List<Report> findByStatus(ReportStatus status);

    default boolean existsByStudentId(String studentId) {
        return !findByStudentId(studentId).isEmpty();
    }
}
