package udehnih.report.repository;

import udehnih.report.model.Report;
import udehnih.report.factory.ReportFactory;
import udehnih.report.enums.ReportStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ReportRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void whenSaveReport_thenReturnSavedReport() {
        Report report = ReportFactory.createOpenReport("12345", "Test Report", "Test Detail");

        Report savedReport = reportRepository.save(report);

        assertThat(savedReport).isNotNull();
        assertThat(savedReport.getReportId()).isNotNull();
        assertThat(savedReport.getStudentId()).isEqualTo("12345");
    }

    @Test
    void whenFindById_thenReturnReport() {
        Report report = ReportFactory.createOpenReport("12345", "Test Report", "Test Detail");
        entityManager.persist(report);
        entityManager.flush();

        Optional<Report> found = reportRepository.findById(report.getReportId());

        assertThat(found).isPresent();
        assertThat(found.get().getStudentId()).isEqualTo("12345");
    }

    @Test
    void whenFindByStudentId_thenReturnReports() {
        Report report1 = ReportFactory.createOpenReport("12345", "Test Report 1", "Test Detail 1");
        Report report2 = ReportFactory.createOpenReport("12345", "Test Report 2", "Test Detail 2");

        entityManager.persist(report1);
        entityManager.persist(report2);
        entityManager.flush();

        List<Report> found = reportRepository.findByStudentId("12345");

        assertThat(found).hasSize(2);
        assertThat(found).allMatch(report -> report.getStudentId().equals("12345"));
    }

    @Test
    void whenDeleteReport_thenReportShouldNotExist() {
        Report report = ReportFactory.createOpenReport("12345", "Test Report", "Test Detail");
        entityManager.persist(report);
        entityManager.flush();

        reportRepository.delete(report);
        Optional<Report> deleted = reportRepository.findById(report.getReportId());

        assertThat(deleted).isEmpty();
    }

    @Test
    void whenUpdateReport_thenReportShouldBeUpdated() {
        Report report = ReportFactory.createOpenReport("12345", "Original Title", "Original Detail");
        entityManager.persist(report);
        entityManager.flush();

        report.setTitle("Updated Title");
        report.setDetail("Updated Detail");
        Report updatedReport = reportRepository.save(report);

        assertThat(updatedReport.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedReport.getDetail()).isEqualTo("Updated Detail");
    }

    @Test
    void whenExistsByStudentId_thenReturnTrueOrFalse() {
        Report report = ReportFactory.createOpenReport("12345", "Test Report", "Test Detail");
        entityManager.persist(report);
        entityManager.flush();

        boolean exists = reportRepository.existsByStudentId("12345");
        boolean notExists = reportRepository.existsByStudentId("99999");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void whenFindByStatus_thenReturnReports() {
        Report openReport1 = ReportFactory.createOpenReport("12345", "Open Report 1", "Detail 1");
        Report openReport2 = ReportFactory.createOpenReport("67890", "Open Report 2", "Detail 2");
        Report resolvedReport = ReportFactory.createOpenReport("11111", "Resolved Report", "Detail 3");
        resolvedReport.setStatus(ReportStatus.RESOLVED);

        entityManager.persist(openReport1);
        entityManager.persist(openReport2);
        entityManager.persist(resolvedReport);
        entityManager.flush();

        List<Report> openReports = reportRepository.findByStatus(ReportStatus.OPEN);
        List<Report> resolvedReports = reportRepository.findByStatus(ReportStatus.RESOLVED);

        assertThat(openReports).hasSize(2);
        assertThat(openReports).allMatch(report -> ReportStatus.OPEN.equals(report.getStatus()));
        assertThat(resolvedReports).hasSize(1);
        assertThat(resolvedReports).allMatch(report -> ReportStatus.RESOLVED.equals(report.getStatus()));
    }
}
