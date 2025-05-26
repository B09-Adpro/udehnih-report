package udehnih.report.repository;
import udehnih.report.model.Report;
import udehnih.report.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    @Async
    @Query("SELECT r FROM Report r WHERE r.studentId = ?1")
    CompletableFuture<List<Report>> findByStudentId(String studentId);
    List<Report> findByStatus(ReportStatus status);
    @Async
    @Query("SELECT r FROM Report r")
    CompletableFuture<List<Report>> findAllAsync();
    Page<Report> findAll(Pageable pageable);
    default boolean existsByStudentId(String studentId) {
        return !findByStudentId(studentId).join().isEmpty();
    }
}
