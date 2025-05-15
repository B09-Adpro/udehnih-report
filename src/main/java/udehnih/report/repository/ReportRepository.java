package udehnih.report.repository;

import udehnih.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    List<Report> findByStudentId(String studentId);

    default boolean existsByStudentId(String studentId) {
        return !findByStudentId(studentId).isEmpty();
    }
}
