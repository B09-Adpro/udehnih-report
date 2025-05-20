package udehnih.report.repository;

import udehnih.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    List<Report> findByUserId(String userId);

    List<Report> findByStatus(String status);

    default boolean existsByUserId(String userId) {
        return !findByUserId(userId).isEmpty();
    }
}
