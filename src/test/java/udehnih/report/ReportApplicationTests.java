package udehnih.report;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import udehnih.report.service.ReportService;
import udehnih.report.util.JwtUtil;
import udehnih.report.service.CustomUserDetailsService;
@SpringBootTest
@ActiveProfiles("test")
public class ReportApplicationTests {
    @Autowired
    private ReportService reportService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Test
    void contextLoads() {
        assert reportService != null : "ReportService should not be null";
        assert jwtUtil != null : "JwtUtil should not be null";
        assert customUserDetailsService != null : "CustomUserDetailsService should not be null";
    }
}
