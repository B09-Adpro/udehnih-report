package udehnih.report;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReportApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainRuns() {
        ReportApplication.main(new String[] {});
    }

    @Test
    void defaultConstructor() {
        new ReportApplication();
    }

}
